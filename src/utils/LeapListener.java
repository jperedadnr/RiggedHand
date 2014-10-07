package utils;

import com.leapmotion.leap.Arm;
import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.FingerList;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Screen;
import com.leapmotion.leap.Vector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;

/**
 *
 * @author Jose Pereda - June 2014 -  @JPeredaDnr
*/
public class LeapListener extends Listener {
    
    private final BooleanProperty doneLeft= new SimpleBooleanProperty(false);
    private final BooleanProperty doneRight= new SimpleBooleanProperty(false);
   
    private final List<Finger> fingersLeft=new ArrayList<>();
    private final List<Finger> fingersRight=new ArrayList<>();
    private final List<Bone> bones=new ArrayList<>();
    private final List<Arm> arms=new ArrayList<>();
    private final List<Pair> joints=new ArrayList<>();
    private final List<Vector> palmsPosition=new ArrayList<>();
    
    private final ObjectProperty<Point3D> posHandLeft=new SimpleObjectProperty<>(Point3D.ZERO);
    private final DoubleProperty pitchLeft=new SimpleDoubleProperty(0d);
    private final DoubleProperty rollLeft=new SimpleDoubleProperty(0d);
    private final DoubleProperty yawLeft=new SimpleDoubleProperty(0d);
    private final ObjectProperty<Point3D> posHandRight=new SimpleObjectProperty<>(Point3D.ZERO);
    private final DoubleProperty pitchRight=new SimpleDoubleProperty(0d);
    private final DoubleProperty rollRight=new SimpleDoubleProperty(0d);
    private final DoubleProperty yawRight=new SimpleDoubleProperty(0d);
    
    private final LimitQueue<Vector> posLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> pitchLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> rollLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> yawLeftAverage = new LimitQueue<>(30);
    private final LimitQueue<Vector> posRightAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> pitchRightAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> rollRightAverage = new LimitQueue<>(30);
    private final LimitQueue<Double> yawRightAverage = new LimitQueue<>(30);
    
    public ObservableValue<Point3D> posHandLeftProperty(){ return posHandLeft; }
    public DoubleProperty yawLeftProperty(){ return yawLeft; }
    public DoubleProperty pitchLeftProperty(){ return pitchLeft; }
    public DoubleProperty rollLeftProperty(){ return rollLeft; }
    public ObservableValue<Point3D> posHandRightProperty(){ return posHandRight; }
    public DoubleProperty yawRightProperty(){ return yawRight; }
    public DoubleProperty pitchRightProperty(){ return pitchRight; }
    public DoubleProperty rollRightProperty(){ return rollRight; }
    
    @Override
    public void onFrame(Controller controller) {
        Frame frame = controller.frame();
        doneLeft.set(false);
        doneRight.set(false);
        bones.clear();
        arms.clear();
        joints.clear();
        fingersRight.clear();
        fingersLeft.clear();
        if (!frame.hands().isEmpty()) {
            Screen screen = controller.locatedScreens().get(0);
            if (screen != null && screen.isValid()){
                for(Finger finger : frame.fingers()){
                    if(finger.isValid()){
                        for(Bone.Type b : Bone.Type.values()) {
                            if((!finger.type().equals(Finger.Type.TYPE_RING) && 
                                !finger.type().equals(Finger.Type.TYPE_MIDDLE)) || 
                                !b.equals(Bone.Type.TYPE_METACARPAL)){
                                bones.add(finger.bone(b));
                            }
                        }
                    }
                }
                for(Hand h: frame.hands()){
                    if(h.isValid()){
                        // arm
                        arms.add(h.arm());
                        if(h.isLeft() && h.isValid()){
                            pitchLeftAverage.add(new Double(h.direction().pitch()));
                            rollLeftAverage.add(new Double(h.palmNormal().roll()));
                            yawLeftAverage.add(new Double(h.direction().yaw()));                   
                            pitchLeft.set(dAverage(pitchLeftAverage));
                            rollLeft.set(dAverage(rollLeftAverage));
                            yawLeft.set(dAverage(yawLeftAverage));

                            Vector intersect = screen.intersect(h.palmPosition(),h.direction(), true);
                            posLeftAverage.add(intersect);
                            Vector avIntersect=vAverage(posLeftAverage);
                            posHandLeft.setValue(new Point3D(screen.widthPixels()*Math.min(1d,Math.max(0d,avIntersect.getX())),
                                    screen.heightPixels()*Math.min(1d,Math.max(0d,(1d-avIntersect.getY()))),
                                    h.palmPosition().getZ()));
                        }
                        if(h.isRight()&& h.isValid()){
                            pitchRightAverage.add(new Double(h.direction().pitch()));
                            rollRightAverage.add(new Double(h.palmNormal().roll()));
                            yawRightAverage.add(new Double(h.direction().yaw()));                   
                            pitchRight.set(dAverage(pitchRightAverage));
                            rollRight.set(dAverage(rollRightAverage));
                            yawRight.set(dAverage(yawRightAverage));

                            Vector intersect = screen.intersect(h.palmPosition(),h.direction(), true);
                            posRightAverage.add(intersect);
                            Vector avIntersect=vAverage(posRightAverage);
                            posHandRight.setValue(new Point3D(screen.widthPixels()*Math.min(1d,Math.max(0d,avIntersect.getX())),
                                    screen.heightPixels()*Math.min(1d,Math.max(0d,(1d-avIntersect.getY()))),
                                    h.palmPosition().getZ()));
                        }
                        
                        FingerList fingers = h.fingers();
                        Finger index=null, middle=null, ring=null, pinky=null;
                        for(Finger f: fingers){
                            if(f.isFinger() && f.isValid()){
                                if(h.isRight()&& h.isValid()){
                                    fingersRight.add(f);
                                } 
                                if(h.isLeft()&& h.isValid()){
                                    fingersLeft.add(f);
                                } 
                                switch(f.type()){
                                    case TYPE_INDEX: index=f; break;
                                    case TYPE_MIDDLE: middle=f; break;
                                    case TYPE_RING: ring=f; break;
                                    case TYPE_PINKY: pinky=f; break;
                                }
                            }
                        }
                        // joints
                        if(index!=null && middle!=null){
                            Pair p=new Pair(index.bone(Bone.Type.TYPE_METACARPAL).nextJoint(),
                                            middle.bone(Bone.Type.TYPE_METACARPAL).nextJoint());
                            joints.add(p);
                        }
                        if(middle!=null && ring!=null){
                            Pair p=new Pair(middle.bone(Bone.Type.TYPE_METACARPAL).nextJoint(),
                                            ring.bone(Bone.Type.TYPE_METACARPAL).nextJoint());
                            joints.add(p);
                        }
                        if(ring!=null && pinky!=null){
                            Pair p=new Pair(ring.bone(Bone.Type.TYPE_METACARPAL).nextJoint(),
                                            pinky.bone(Bone.Type.TYPE_METACARPAL).nextJoint());
                            joints.add(p);
                        }
                        if(index!=null && pinky!=null){
                            Pair p=new Pair(index.bone(Bone.Type.TYPE_METACARPAL).prevJoint(),
                                            pinky.bone(Bone.Type.TYPE_METACARPAL).prevJoint());
                            joints.add(p);
                        }        
                    }
                }
            }
        }
        
        doneLeft.set(!fingersLeft.isEmpty());
        doneRight.set(!fingersRight.isEmpty());
    }
    
    public List<Finger> getFingersRight(){ 
        return fingersRight.stream().collect(Collectors.toList());
    }
    public List<Finger> getFingersLeft(){ 
        return fingersLeft.stream().collect(Collectors.toList());
    }
    public List<Bone> getBones(){ 
        return bones.stream().collect(Collectors.toList());
    }
    public List<Arm> getArms(){ 
        return arms.stream().collect(Collectors.toList());
    }
    public List<Pair> getJoints(){ 
        return joints.stream().collect(Collectors.toList());
    }
    
    public BooleanProperty doneLeftProperty() { 
        return doneLeft; 
    }
    public BooleanProperty doneRightProperty() { 
        return doneRight; 
    }
    
    public List<Vector> getPalmsPosition(){ return palmsPosition; }
    
    private Vector vAverage(LimitQueue<Vector> vectors){
        float vx=0f, vy=0f, vz=0f;
        for(Vector v:vectors){
            vx=vx+v.getX(); 
            vy=vy+v.getY(); 
            vz=vz+v.getZ();
        }
        return new Vector(vx/vectors.size(), vy/vectors.size(), vz/vectors.size());
    }
    
    private Double dAverage(LimitQueue<Double> vectors){
        double vx=0;
        for(Double d:vectors){
            vx=vx+d;
        }
        return vx/vectors.size();
    }
    
    private class LimitQueue<E> extends LinkedList<E> {
        private final int limit;
        public LimitQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            super.add(o);
            while (size() > limit) { super.remove(); }
            return true;
        }
    }
}
