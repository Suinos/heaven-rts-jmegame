/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rts.cam;

/**
 *
 * @author cuong.nguyenmanh2
 */
import java.io.IOException;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

public class RTSCamera implements Control, ActionListener {

    public enum Degree {

        SIDE,
        FWD,
        ROTATE,
        TILT,
        DISTANCE
    }
    private InputManager inputManager;
    private final Camera cam;
    private int[] direction = new int[5];
    private float[] accelPeriod = new float[5];
    private float[] maxSpeed = new float[5];
    private float[] maxAccelPeriod = new float[5];
    private float[] minValue = new float[5];
    private float[] maxValue = new float[5];
    private Vector3f position = new Vector3f();
    private Vector3f center = new Vector3f();
    private float tilt = (float) (Math.PI / 4);
    private float rot = 0;
    private float distance = 15;
    private static final int SIDE = Degree.SIDE.ordinal();
    private static final int FWD = Degree.FWD.ordinal();
    private static final int ROTATE = Degree.ROTATE.ordinal();
    private static final int TILT = Degree.TILT.ordinal();
    private static final int DISTANCE = Degree.DISTANCE.ordinal();

    public RTSCamera(Camera cam, Spatial target) {
        this.cam = cam;

        setMinMaxValues(Degree.SIDE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.FWD, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.ROTATE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.TILT, 0.2f, (float) (Math.PI / 2) - 0.001f);
        setMinMaxValues(Degree.DISTANCE, 2, Float.POSITIVE_INFINITY);

        setMaxSpeed(Degree.SIDE, 10f, 0.4f);
        setMaxSpeed(Degree.FWD, 10f, 0.4f);
        setMaxSpeed(Degree.ROTATE, 2f, 0.4f);
        setMaxSpeed(Degree.TILT, 1f, 0.4f);
        setMaxSpeed(Degree.DISTANCE, 15f, 0.4f);
        target.addControl(this);
    }

    public void setMaxSpeed(Degree deg, float maxSpd, float accelTime) {
        maxSpeed[deg.ordinal()] = maxSpd / accelTime;
        maxAccelPeriod[deg.ordinal()] = accelTime;
    }

    public void registerWithInput(InputManager inputManager) {
        this.inputManager = inputManager;

        String[] mappings = new String[]{"+SIDE", "+FWD", "+ROTATE", "+TILT", "+DISTANCE",
            "-SIDE", "-FWD", "-ROTATE", "-TILT", "-DISTANCE",};

        inputManager.addMapping("-SIDE", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("+SIDE", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("+FWD", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("-FWD", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("+ROTATE", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("-ROTATE", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("+TILT", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("-TILT", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("-DISTANCE", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("+DISTANCE", new KeyTrigger(KeyInput.KEY_X));

        inputManager.addListener(this, mappings);
        inputManager.setCursorVisible(true);
    }

    public void write(JmeExporter ex) throws IOException {
    }

    public void read(JmeImporter im) throws IOException {
    }

    public Control cloneForSpatial(Spatial spatial) {
        RTSCamera other = new RTSCamera(cam, spatial);
        other.registerWithInput(inputManager);
        return other;
    }

    public void setSpatial(Spatial spatial) {
    }

    public void setEnabled(boolean enabled) {
    }

    public boolean isEnabled() {

        return true;
    }

    public void update(final float tpf) {

        for (int i = 0; i < direction.length; i++) {
            int dir = direction[i];
            switch (dir) {
                case -1:
                    accelPeriod[i] = clamp(-maxAccelPeriod[i], accelPeriod[i] - tpf, accelPeriod[i]);
                    break;
                case 0:
                    if (accelPeriod[i] != 0) {
                        double oldSpeed = accelPeriod[i];
                        if (accelPeriod[i] > 0) {
                            accelPeriod[i] -= tpf;
                        } else {
                            accelPeriod[i] += tpf;
                        }
                        if (oldSpeed * accelPeriod[i] < 0) {
                            accelPeriod[i] = 0;
                        }
                    }
                    break;
                case 1:
                    accelPeriod[i] = clamp(accelPeriod[i], accelPeriod[i] + tpf, maxAccelPeriod[i]);
                    break;
            }

        }


        distance += maxSpeed[DISTANCE] * accelPeriod[DISTANCE] * tpf;
        tilt += maxSpeed[TILT] * accelPeriod[TILT] * tpf;
        rot += maxSpeed[ROTATE] * accelPeriod[ROTATE] * tpf;

        distance = clamp(minValue[DISTANCE], distance, maxValue[DISTANCE]);
        rot = clamp(minValue[ROTATE], rot, maxValue[ROTATE]);
        tilt = clamp(minValue[TILT], tilt, maxValue[TILT]);

        double offX = maxSpeed[SIDE] * accelPeriod[SIDE] * tpf;
        double offZ = maxSpeed[FWD] * accelPeriod[FWD] * tpf;

        center.x += offX * Math.cos(-rot) + offZ * Math.sin(rot);
        center.z += offX * Math.sin(-rot) + offZ * Math.cos(rot);

        position.x = center.x + (float) (distance * Math.cos(tilt) * Math.sin(rot));
        position.y = center.y + (float) (distance * Math.sin(tilt));
        position.z = center.z + (float) (distance * Math.cos(tilt) * Math.cos(rot));


        cam.setLocation(position);
        cam.lookAt(center, new Vector3f(0, 1, 0));


    }

    private static float clamp(float min, float value, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public float getMaxSpeed(Degree dg) {
        return maxSpeed[dg.ordinal()];
    }

    public float getMinValue(Degree dg) {
        return minValue[dg.ordinal()];
    }

    public float getMaxValue(Degree dg) {
        return maxValue[dg.ordinal()];
    }

    // SIDE and FWD min/max values are ignored
    public void setMinMaxValues(Degree dg, float min, float max) {
        minValue[dg.ordinal()] = min;
        maxValue[dg.ordinal()] = max;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        int press = isPressed ? 1 : 0;

        char sign = name.charAt(0);
        if (sign == '-') {
            press = -press;
        } else if (sign != '+') {
            return;
        }

        Degree deg = Degree.valueOf(name.substring(1));
        direction[deg.ordinal()] = press;
    }
}
