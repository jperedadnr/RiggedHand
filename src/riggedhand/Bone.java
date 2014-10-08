/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package riggedhand;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 *
 * @author jpereda
 */
public class Bone extends Group {

    public Bone(){
        this(1, Point3D.ZERO);
    }
    public Bone(double scale, Point3D posJoint) {
        Box origin=new Box(10,10,10);
        origin.setMaterial(new PhongMaterial(Color.ORANGE));
        
        Cylinder bone = new Cylinder(5, posJoint.magnitude()/scale);
        double angle = Math.toDegrees(Math.acos((new Point3D(0,1,0)).dotProduct(posJoint)/posJoint.magnitude()));
        Point3D axis = (new Point3D(0,1,0)).crossProduct(posJoint);
        bone.getTransforms().addAll(new Rotate(angle,0,0,0,axis), new Translate(0,posJoint.magnitude()/2d/scale, 0));
        bone.setMaterial(new PhongMaterial(Color.CADETBLUE));
        
        Sphere end = new Sphere(6);
        end.getTransforms().addAll(new Translate(posJoint.getX()/scale,posJoint.getY()/scale,posJoint.getZ()/scale));
        end.setMaterial(new PhongMaterial(Color.YELLOW));
        
        getChildren().addAll(origin, bone, end);
        getTransforms().add(new Scale(scale, scale, scale));
    }
    
}
