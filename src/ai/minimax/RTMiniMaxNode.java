/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.minimax;

import mrts.GameState;
import mrts.PlayerAction;
import mrts.PlayerActionGenerator;
import util.Pair;

/**
 *
 * @author santi
 */
public class RTMiniMaxNode {
    public int type;    // -1: unknown, 0 : max, 1 : min, 2: simulation
    public GameState gs;
    public PlayerActionGenerator actions;
    public float alpha, beta;
    public Pair<PlayerAction,Float> best;
    
    public RTMiniMaxNode(int a_type, GameState a_gs, float a_alpha, float a_beta) {
        type = a_type;
        gs = a_gs;
        alpha = a_alpha;
        beta = a_beta;
    }
}
