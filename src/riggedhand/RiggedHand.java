package riggedhand;

import utils.LeapListener;
import com.javafx.experiments.importers.maya.Joint;
import com.javafx.experiments.shape3d.PolygonMeshView;
import com.javafx.experiments.shape3d.SkinningMesh;
import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import utils.DragSupport;

/** JavaFX Application displaying hands 3D models, and performing 
 * mesh deformations (skinning) through the Leap Motion controller and its
 * skeletal tracking version (v2)
 *
 * @author Alexander Kouznetsov
 * @author José Pereda
 * 
 * September 2014 JavaOne
 * 
 * More:
 * - http://hg.openjdk.java.net/openjfx/8u-dev/rt/file/7c86f6fc6423/apps/samples/3DViewer
 * - https://github.com/leapmotion/leapjs-rigged-hand
 * 
 */
public class RiggedHand extends Application {

    private final Translate translate = new Translate(0, 0, 0);
    private final Translate translateZ = new Translate(0, 0, -1070);
    private final Rotate rotateX = new Rotate(-120, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(180, 0, 0, 0, Rotate.Y_AXIS);
    private final Translate translateY = new Translate(0, 0, 0);
    
    private PolygonMeshView skinningRight;
    private List<Parent> forestRight = new ArrayList<>();
    private PolygonMeshView skinningLeft;
    private List<Parent> forestLeft = new ArrayList<>();
    
    private LeapListener listener = null;
    private Controller controller = null;
    private Bone previousBone=null;
    private final double leapScale=20d;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        listener = new LeapListener();
        controller = new Controller();
        controller.addListener(listener);
        
//        HandImporter handLeft=new HandImporter("modelLeft.json");
        /*
        Model downloaded from
        https://github.com/leapmotion/leapjs-rigged-hand/tree/master/src/models
        */
//        HandImporter handLeft=new HandImporter("left_hand_terrence_3.js");
        HandImporter handLeft=new HandImporter("modelLeft.json",true,false);
        handLeft.readModel();

        forestLeft=handLeft.getJointForest();
        skinningLeft=handLeft.getSkinningMeshView();
        
        /* 
        Model downloaded from 
        https://github.com/leapmotion/leapjs-rigged-hand/blob/master/src/models/hand_models_v1.js
        */
        HandImporter handRight=new HandImporter("modelRight.json",false,false);
        handRight.readModel();
        forestRight=handRight.getJointForest();
        skinningRight=handRight.getSkinningMeshView();
        
        Group root = new Group(new Group(skinningLeft, forestLeft.get(0)),
                               new Group(skinningRight, forestRight.get(0)));
        
        listener.doneLeftProperty().addListener((ov,b,b1)->{
            if(b1){
                List<Finger> fingersLeft=listener.getFingersLeft();
                Platform.runLater(()->{
                    fingersLeft.stream()
                        .filter(finger -> finger.isValid())
                        .forEach(finger -> {
                            previousBone=null;
                            Stream.of(Bone.Type.values()).map(finger::bone)
                                .filter(bone -> bone.isValid() && bone.length()>0)
                                .forEach(bone -> {
                                    if(previousBone!=null){
                                        Joint joint = getJoint(false,finger,bone);
                                        Vector cross = bone.direction().cross(previousBone.direction());
                                        double angle = bone.direction().angleTo(previousBone.direction());
                                        joint.rx.setAngle(Math.toDegrees(angle));
                                        joint.rx.setAxis(new Point3D(cross.getX(),-cross.getY(),cross.getZ()));
                                    }
                                    previousBone=bone;
                            });
                    });
                    matrixRotateNode(false, listener.rollLeftProperty().get(),
                            listener.pitchLeftProperty().get(),
                            listener.yawLeftProperty().get());
                    
                    Point3D moveLeft = listener.posHandLeftProperty().getValue().multiply(1d/leapScale);
                    ((Joint)forestLeft.get(0)).t.setX(2-moveLeft.getX());
                    ((Joint)forestLeft.get(0)).t.setY(moveLeft.getY());
                    ((Joint)forestLeft.get(0)).t.setZ(-moveLeft.getZ());
                    
                    ((SkinningMesh)skinningLeft.getMesh()).update();
                });
            }
        });
        listener.doneRightProperty().addListener((ov,b,b1)->{
            if(b1){
                List<Finger> fingersRight=listener.getFingersRight();
                Platform.runLater(()->{
                    fingersRight.stream()
                        .filter(finger -> finger.isValid())
                        .forEach(finger -> {
                            previousBone=null;
                            Stream.of(Bone.Type.values()).map(finger::bone)
                                .filter(bone -> bone.isValid() && bone.length()>0)
                                .forEach(bone -> {
                                    if(previousBone!=null){
                                        Joint joint = getJoint(true,finger,bone);
                                        Vector cross = bone.direction().cross(previousBone.direction());
                                        double angle = bone.direction().angleTo(previousBone.direction());
                                        joint.rx.setAngle(Math.toDegrees(angle));
                                        joint.rx.setAxis(new Point3D(cross.getX(),-cross.getY(),cross.getZ()));
                                    }
                                    previousBone=bone;
                            });
                        });

                    matrixRotateNode(true, listener.rollRightProperty().get(),
                            listener.pitchRightProperty().get(),
                            listener.yawRightProperty().get());
                    Point3D moveRight = listener.posHandRightProperty().getValue().multiply(1d/leapScale);
                    ((Joint)forestRight.get(0)).t.setX(-2-moveRight.getX());
                    ((Joint)forestRight.get(0)).t.setY(moveRight.getY());
                    ((Joint)forestRight.get(0)).t.setZ(-moveRight.getZ());
                    
                    ((SkinningMesh)skinningRight.getMesh()).update();                    
                });
            }
        });
        
        Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
        perspectiveCamera.setNearClip(0.001);
        perspectiveCamera.setFarClip(10000);
        scene.setCamera(perspectiveCamera);
        primaryStage.setScene(scene);
        primaryStage.setTitle("RIGGED HANDS - JAVAFX 3D");
        primaryStage.show();
        
        Translate centerTranslate = new Translate();
        centerTranslate.xProperty().bind(scene.widthProperty().divide(2));
        centerTranslate.yProperty().bind(scene.heightProperty().divide(2));
        
        scene.getRoot().getTransforms().addAll(centerTranslate, translate, translateZ, rotateX, rotateY, translateY);
        
        DragSupport dragSupport = new DragSupport(scene, null, MouseButton.SECONDARY, Orientation.VERTICAL, translateZ.zProperty(), -3);
        DragSupport dragSupport1 = new DragSupport(scene, null, Orientation.HORIZONTAL, rotateY.angleProperty());
        DragSupport dragSupport2 = new DragSupport(scene, null, Orientation.VERTICAL, rotateX.angleProperty());
        DragSupport dragSupport3 = new DragSupport(scene, null, MouseButton.MIDDLE, Orientation.HORIZONTAL, translate.xProperty());
        DragSupport dragSupport4 = new DragSupport(scene, null, MouseButton.MIDDLE, Orientation.VERTICAL, translate.yProperty());
        
        ((Joint)forestLeft.get(0)).t.setX(4);
        ((SkinningMesh)skinningLeft.getMesh()).update();  
        ((Joint)forestRight.get(0)).t.setX(-4);
        ((SkinningMesh)skinningRight.getMesh()).update();  
    }
    
    @Override
    public void stop(){
        controller.removeListener(listener);
    }

    /*
    Mapping between bones and fingers from Leap Motion and those from the js model
    Checks two possible patterns
    */
    private Joint getJoint(boolean right, Finger finger, Bone bone){
        int f = 0,b = 0;
        String name="";
        switch(finger.type()){
            case TYPE_THUMB: name="thumb"; f=0; break;
            case TYPE_INDEX: name="index"; f=1; break;
            case TYPE_MIDDLE: name="middle"; f=2; break;
            case TYPE_RING: name="ring"; f=3; break;
            case TYPE_PINKY: name="pinky"; f=4; break;
        }
        switch(bone.type()){
            case TYPE_METACARPAL: b=0; break;
            case TYPE_PROXIMAL: b=1; break;
            case TYPE_INTERMEDIATE: b=2; break;
            case TYPE_DISTAL: b=3; break;
        }
        String bonePattern1="#Finger_"+Integer.toString(f)+Integer.toString(b-1);
        String bonePattern2="#"+name+"-"+Integer.toString(b-1);
        if(right){
            Joint joint = (Joint)forestRight.get(0).lookup(bonePattern1);
            if(joint==null){
                joint = (Joint)forestRight.get(0).lookup(bonePattern2);
            }
            return joint;
        } 
        Joint joint = (Joint)forestLeft.get(0).lookup(bonePattern1);
        if(joint==null){
            joint = (Joint)forestLeft.get(0).lookup(bonePattern2);
        }
        return joint;
    }
    
    /*
    http://jperedadnr.blogspot.com/2013/06/leap-motion-controller-and-javafx-new.html
    */
    private void matrixRotateNode(boolean right, double alf, double bet, double gam){
        double A11=Math.cos(alf)*Math.cos(gam);
        double A12=Math.cos(bet)*Math.sin(alf)+Math.cos(alf)*Math.sin(bet)*Math.sin(gam);
        double A13=Math.sin(alf)*Math.sin(bet)-Math.cos(alf)*Math.cos(bet)*Math.sin(gam);
        double A21=-Math.cos(gam)*Math.sin(alf);
        double A22=Math.cos(alf)*Math.cos(bet)-Math.sin(alf)*Math.sin(bet)*Math.sin(gam);
        double A23=Math.cos(alf)*Math.sin(bet)+Math.cos(bet)*Math.sin(alf)*Math.sin(gam);
        double A31=Math.sin(gam);
        double A32=-Math.cos(gam)*Math.sin(bet);
        double A33=Math.cos(bet)*Math.cos(gam);
         
        double d = Math.acos((A11+A22+A33-1d)/2d);
        if(d!=0d){
            double den=2d*Math.sin(d);
            Point3D p= new Point3D((A32-A23)/den,(A13-A31)/den,(A21-A12)/den);
            if(right){
                ((Joint)forestRight.get(0)).rx.setAxis(p);
                ((Joint)forestRight.get(0)).rx.setAngle(Math.toDegrees(d));                   
            } else {
                ((Joint)forestLeft.get(0)).rx.setAxis(p);
                ((Joint)forestLeft.get(0)).rx.setAngle(Math.toDegrees(d));                   
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}