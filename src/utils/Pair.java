package utils;

import com.leapmotion.leap.Vector;

/**
 *
 * @author Jose Pereda - August 2014 -  @JPeredaDnr
 */
public class Pair {
    
    /*
    Creates a pair of joints (in terms of Vectors) to join the proximal end of two bones
    */
    private Vector v0;
    private Vector v1;
    
    public Pair(Vector v0, Vector v1){
        this.v0=v0;
        this.v1=v1;
    }

    public Vector getV0() {
        return v0;
    }

    public void setV0(Vector v0) {
        this.v0 = v0;
    }

    public Vector getV1() {
        return v1;
    }

    public void setV1(Vector v1) {
        this.v1 = v1;
    }

    public Vector getCenter(){
        return new Vector((v1.getX()+v0.getX())/2f,(v1.getY()+v0.getY())/2f,(v1.getZ()+v0.getZ())/2f);
    }
    
    public Vector getDirection(){
        return new Vector(v1.getX()-v0.getX(),v1.getY()-v0.getY(),v1.getZ()-v0.getZ()).normalized();
    }                      
    
    @Override
    public String toString() {
        return "Pair{" + "v0=" + v0 + ", v1=" + v1 + '}';
    }
    
}
