/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import ai.*;
import ai.montecarlo.*;
import ai.mcts.ContinuingNaiveMCTS;
import ai.minimax.IDContinuingRTMinimax;
import ai.minimax.IDContinuingRTMinimax;
import ai.minimax.IDContinuingRTMinimaxRandomized;
import ai.mcts.uct.UCT;
import gui.PhysicalGameStatePanel;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import mrts.GameState;
import mrts.PhysicalGameState;
import mrts.PlayerAction;
import mrts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
public class Experimenter {
    public static int DEBUG = 0;
    
    public static void runExperiments(List<AI> bots, List<PhysicalGameState> maps, int iterations, int max_cycles, int max_inactive_cycles, boolean visualize) throws Exception {
        runExperiments(bots, maps, iterations, max_cycles, max_inactive_cycles, visualize, System.out, -1);
    }

    public static void runExperiments(List<AI> bots, List<PhysicalGameState> maps, int iterations, int max_cycles, int max_inactive_cycles, boolean visualize, PrintStream out) throws Exception {
        runExperiments(bots, maps, iterations, max_cycles, max_inactive_cycles, visualize, out, -1);
    }

    public static void runExperiments(List<AI> bots, List<PhysicalGameState> maps, int iterations, int max_cycles, int max_inactive_cycles, boolean visualize, PrintStream out, int run_only_those_involving_this_AI) throws Exception {
        int wins[][] = new int[bots.size()][bots.size()];
        int ties[][] = new int[bots.size()][bots.size()];
        int loses[][] = new int[bots.size()][bots.size()];
        
        double win_time[][] = new double[bots.size()][bots.size()];
        double tie_time[][] = new double[bots.size()][bots.size()];
        double lose_time[][] = new double[bots.size()][bots.size()];

        List<AI> bots2 = new LinkedList<AI>();
        for(AI bot:bots) bots2.add(bot.clone());
        
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) 
        {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) 
            {
                if (run_only_those_involving_this_AI!=-1 &&
                    ai1_idx!=run_only_those_involving_this_AI &&
                    ai2_idx!=run_only_those_involving_this_AI) continue;
//                if (ai1_idx==0 && ai2_idx==0) continue;
                
                for(PhysicalGameState pgs:maps) {
                    
                    for (int i = 0; i < iterations; i++) {
                        AI ai1 = bots.get(ai1_idx);
                        AI ai2 = bots2.get(ai2_idx);
                        long lastTimeActionIssued = 0;

                        ai1.reset();
                        ai2.reset();

                        GameState gs = new GameState(pgs.clone(),UnitTypeTable.utt);
                        JFrame w = null;
                        if (visualize) w = PhysicalGameStatePanel.newVisualizer(gs, 600, 600);

                        out.println("MATCH UP: " + ai1+ " vs " + ai2);
                        
                        boolean gameover = false;
                        do {
                            System.gc();
                            PlayerAction pa1 = ai1.getAction(0, gs);
                            if (DEBUG>=1) {out.println("AI1 done.");out.flush();}
                            PlayerAction pa2 = ai2.getAction(1, gs);
                            if (DEBUG>=1) {out.println("AI2 done.");out.flush();}
                            if (gs.issueSafe(pa1)) lastTimeActionIssued = gs.getTime();
                            if (DEBUG>=1) {out.println("issue action AI1 done.");out.flush();}
                            if (gs.issueSafe(pa2)) lastTimeActionIssued = gs.getTime();
                            if (DEBUG>=1) {out.println("issue action AI2 done.");out.flush();}
                            gameover = gs.cycle();
                            if (DEBUG>=1) {out.println("cycle done.");out.flush();}
                            if (w!=null) w.repaint();
                            if (DEBUG>=1) {out.println("repaint done.");out.flush();}
                            try {
                                Thread.sleep(1);    // give time to the window to repaint
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } while (!gameover && 
                                 (gs.getTime() < max_cycles) && 
                                 (gs.getTime() - lastTimeActionIssued < max_inactive_cycles));
                        if (w!=null) w.dispose();
                        int winner = gs.winner();
                        out.println("Winner: " + winner + "  in " + gs.getTime() + " cycles");
                        out.println(ai1 + " : " + ai1.statisticsString());
                        out.println(ai2 + " : " + ai2.statisticsString());
                        out.flush();
                        if (winner == -1) {
                            ties[ai1_idx][ai2_idx]++;
                            tie_time[ai1_idx][ai2_idx]+=gs.getTime();

                            ties[ai2_idx][ai1_idx]++;
                            tie_time[ai2_idx][ai1_idx]+=gs.getTime();
                        } else if (winner == 0) {
                            wins[ai1_idx][ai2_idx]++;
                            win_time[ai1_idx][ai2_idx]+=gs.getTime();

                            loses[ai2_idx][ai1_idx]++;
                            lose_time[ai2_idx][ai1_idx]+=gs.getTime();
                        } else if (winner == 1) {
                            loses[ai1_idx][ai2_idx]++;
                            lose_time[ai1_idx][ai2_idx]+=gs.getTime();

                            wins[ai2_idx][ai1_idx]++;
                            win_time[ai2_idx][ai1_idx]+=gs.getTime();
                        }                        
                    }                    
                }
            }
        }

        out.println("Wins: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                out.print(wins[ai1_idx][ai2_idx] + ", ");
            }
            out.println("");
        }
        out.println("Ties: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                out.print(ties[ai1_idx][ai2_idx] + ", ");
            }
            out.println("");
        }
        out.println("Loses: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                out.print(loses[ai1_idx][ai2_idx] + ", ");
            }
            out.println("");
        }        
        out.println("Win average time: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                if (wins[ai1_idx][ai2_idx]>0) {
                    out.print((win_time[ai1_idx][ai2_idx]/wins[ai1_idx][ai2_idx]) + ", ");
                } else {
                    out.print("-, ");
                }
            }
            out.println("");
        }
        out.println("Tie average time: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                if (ties[ai1_idx][ai2_idx]>0) {
                    out.print((tie_time[ai1_idx][ai2_idx]/ties[ai1_idx][ai2_idx]) + ", ");
                } else {
                    out.print("-, ");
                }
            }
            out.println("");
        }
        out.println("Lose average time: ");
        for (int ai1_idx = 0; ai1_idx < bots.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < bots.size(); ai2_idx++) {
                if (loses[ai1_idx][ai2_idx]>0) {
                    out.print((lose_time[ai1_idx][ai2_idx]/loses[ai1_idx][ai2_idx]) + ", ");
                } else {
                    out.print("-, ");
                }
            }
            out.println("");
        }              
        out.flush();
    }
}
