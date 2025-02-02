package rts.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import java.util.ArrayList;
import java.util.List;

public class CannonShooterTest extends SimpleApplication implements AnalogListener,
        ActionListener {

    private BulletAppState bulletAppState;
    private PhysicsHoverControl hoverControl;
    private Spatial spaceCraft;
    TerrainQuad terrain;
    Material matRock;
    boolean wireframe = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    Geometry collisionMarker;

    public static void main(String[] args) {
        CannonShooterTest app = new CannonShooterTest();
        app.start();
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
//        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        bulletAppState.getPhysicsSpace().setAccuracy(1f / 30f);
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

        PssmShadowRenderer pssmr = new PssmShadowRenderer(assetManager, 2048, 3);
        pssmr.setDirection(new Vector3f(-0.5f, -0.3f, -0.3f).normalizeLocal());
        pssmr.setLambda(0.55f);
        pssmr.setShadowIntensity(0.6f);
        pssmr.setCompareMode(CompareMode.Hardware);
        pssmr.setFilterMode(FilterMode.Bilinear);
        //viewPort.addProcessor(pssmr);

        setupKeys();
        createTerrain();
        createSpaceCraft();

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(new ColorRGBA(1.0f, 0.94f, 0.8f, 1f).multLocal(1.3f));
        dl.setDirection(new Vector3f(-0.5f, -0.3f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        Vector3f lightDir2 = new Vector3f(0.70518064f, 0.5902297f, -0.39287305f);
        DirectionalLight dl2 = new DirectionalLight();
        dl2.setColor(new ColorRGBA(0.7f, 0.85f, 1.0f, 1f));
        dl2.setDirection(lightDir2);
        //rootNode.addLight(dl2);
    }

    private void createSpaceCraft() {
        spaceCraft = assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        //CollisionShape colShape = CollisionShapeFactory.createDynamicMeshShape(spaceCraft);
        CollisionShape colShape = CollisionShapeFactory.createBoxShape(spaceCraft);
        spaceCraft.setShadowMode(ShadowMode.CastAndReceive);
        spaceCraft.setLocalTranslation(new Vector3f(-140, 14, -23));
        spaceCraft.setLocalRotation(new Quaternion(new float[]{0, 0.01f, 0}));

        hoverControl = new PhysicsHoverControl(colShape, 50);
        hoverControl.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);

        spaceCraft.addControl(hoverControl);


        rootNode.attachChild(spaceCraft);
        getPhysicsSpace().add(hoverControl);

        ChaseCamera chaseCam = new ChaseCamera(cam, inputManager);
        chaseCam.setDefaultDistance(70f);
        chaseCam.setDefaultHorizontalRotation(50 * FastMath.DEG_TO_RAD);
        spaceCraft.addControl(chaseCam);

        flyCam.setEnabled(false);
    }

    public void makeMissile() {
        Vector3f pos = spaceCraft.getWorldTranslation().clone();
        Quaternion rot = spaceCraft.getWorldRotation();
        Vector3f dir = rot.getRotationColumn(2);

        Node missile = (Node) assetManager.loadModel("Models/SpaceCraft/Rocket.mesh.xml");
        missile.scale(0.5f);
        missile.rotate(0, FastMath.PI, 0);


        BoundingBox box = (BoundingBox) missile.getWorldBound();
        final Vector3f extent = box.getExtent(null);

        BoxCollisionShape boxShape = new BoxCollisionShape(extent);

        missile.setName("Missile");
        missile.rotate(rot);
        missile.setLocalTranslation(pos.addLocal(0, extent.y * 4.5f, 0));
        missile.setLocalRotation(hoverControl.getPhysicsRotation());
        missile.setShadowMode(ShadowMode.Cast);
        BombControl bombControl = new BombControl(assetManager, boxShape, 20);
        bombControl.setLinearVelocity(dir.mult(100));
        bombControl.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_03);
        missile.addControl(bombControl);
        bombControl.setExplosionSize(8f);
        // Attack a fire ball 
        // Attack smoke
        /** Uses Texture from jme3-test-data library! */
        ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fire.setMaterial(mat_red);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation


        // Flame - Red
        //fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        //fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow

        // Laser - Blue
        fire.setEndColor(new ColorRGBA(0f, 0f, 1f, 1f));   // blue
        fire.setStartColor(new ColorRGBA(0.05f, 0.4f, 0.9f, 0.5f)); // cyan

        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(3.6f);
        fire.setEndSize(0.1f);
        fire.setGravity(0f, 0f, 0f);
        fire.setLowLife(0.5f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);

        missile.attachChild(fire);


        rootNode.attachChild(missile);
        getPhysicsSpace().add(missile);
    }

    public void onAnalog(String binding, float value, float tpf) {
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            hoverControl.steer(value ? 50f : 0);
        } else if (binding.equals("Rights")) {
            hoverControl.steer(value ? -50f : 0);
        } else if (binding.equals("Ups")) {
            hoverControl.accelerate(value ? 100f : 0);
        } else if (binding.equals("Downs")) {
            hoverControl.accelerate(value ? -100f : 0);
        } else if (binding.equals("Reset")) {
            if (value) {
                System.out.println("Reset");
                hoverControl.setPhysicsLocation(new Vector3f(-140, 14, -23));
                hoverControl.setPhysicsRotation(new Matrix3f());
                hoverControl.clearForces();
            } else {
            }
        } else if (binding.equals("Space") && value) {
            makeMissile();
        }
    }

    public void updateCamera() {


        Vector3f pos = spaceCraft.getWorldTranslation().clone();
        Quaternion rot = spaceCraft.getWorldRotation();
        Vector3f dir = rot.getRotationColumn(2);

        // make it XZ only
        Vector3f camPos = new Vector3f(dir);
        camPos.setY(0);
        camPos.normalizeLocal();

        // negate and multiply by distance from object
        camPos.negateLocal();
        camPos.multLocal(15);

        // add Y distance
        camPos.setY(2);
        camPos.addLocal(pos);
        cam.setLocation(camPos);

        Vector3f lookAt = new Vector3f(dir);
        lookAt.multLocal(7); // look at dist
        lookAt.addLocal(pos);
        cam.lookAt(lookAt, Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    private void createTerrain() {
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);
        matRock.setBoolean("WardIso", true);
        matRock.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap", grass);
        matRock.setFloat("DiffuseMap_0_scale", 64);
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_1", dirt);
        matRock.setFloat("DiffuseMap_1_scale", 16);
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_2", rock);
        matRock.setFloat("DiffuseMap_2_scale", 128);
        Texture normalMap0 = assetManager.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
        normalMap0.setWrap(WrapMode.Repeat);
        Texture normalMap1 = assetManager.loadTexture("Textures/Terrain/splat/dirt_normal.png");
        normalMap1.setWrap(WrapMode.Repeat);
        Texture normalMap2 = assetManager.loadTexture("Textures/Terrain/splat/road_normal.png");
        normalMap2.setWrap(WrapMode.Repeat);
        matRock.setTexture("NormalMap", normalMap0);
        matRock.setTexture("NormalMap_1", normalMap2);
        matRock.setTexture("NormalMap_2", normalMap2);

        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
            heightmap.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());

        List<Camera> cameras = new ArrayList<Camera>();
        cameras.add(getCamera());
        TerrainLodControl control = new TerrainLodControl(terrain, cameras);
        terrain.addControl(control);
        terrain.setMaterial(matRock);
        terrain.setLocalScale(new Vector3f(2, 2, 2));
        //terrain.setLocked(false); // unlock it so we can edit the height

        //terrain.setShadowMode(ShadowMode.CastAndReceive);
        terrain.addControl(new RigidBodyControl(0));
        rootNode.attachChild(terrain);
        getPhysicsSpace().addAll(terrain);

    }
}
