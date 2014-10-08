package riggedhand;

import com.javafx.experiments.importers.SmoothingGroups;
import com.javafx.experiments.importers.maya.Joint;
import com.javafx.experiments.shape3d.PolygonMesh;
import com.javafx.experiments.shape3d.PolygonMeshView;
import com.javafx.experiments.shape3d.SkinningMesh;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Translate;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;
import utils.Axes;

/**
 *
 * @author Alexander Kouznetsov
 * Modified by José Pereda
 */
public class HandImporter {

    private final JsonReader reader;
    private final List<Parent> jointForest = new ArrayList<>();
    private PolygonMeshView skinningMeshView;
    
    private final boolean debug=true;
    private final boolean skeletal;
    private final boolean axes;
    
    /** HandImporter provides a way to import THREE.js models in JSON format, formatVersion 3.1
     * https://github.com/mrdoob/three.js/wiki/JSON-Model-format-3
     * and convert them into a Skinning Mesh
     * 
     * faces
     * - triangle with material, vertex uvs and vertex normals: Bitmask 00 10 10 10 = 42
     *  Len 11: {42, vertex_index, vertex_index, vertex_index,
     *    material_index, vertex_uv, vertex_uv, vertex_uv, vertex_normal, vertex_normal, vertex_normal}
     * 
     * - triangle with material and vertext normals: Bitmask 00 10 00 10 = 34
     *  Len 8: {34, vertex_index, vertex_index, vertex_index,
     *    material_index, vertex_normal, vertex_normal, vertex_normal}
     * 
     * @param nameFile file with JSON format
     * @param skeletal hide or show bones(joints)
     * @param axes hide or show local coordinate systems on joints
     */
    public HandImporter(String nameFile, boolean skeletal, boolean axes){
        this.skeletal=skeletal;
        this.axes=axes;
        reader = JsonProvider.provider().createReader(HandImporter.class.getResourceAsStream("/resources/"+nameFile));
    }
    
    public void readModel(){
        readModel(1f);
    }  
    
    /**
     * @param scale Scale up or down the model, by scaling vertices and joints coordinates  
    */
    public void readModel(float scale){
        if(reader==null){
            return;
        }
        
        JsonObject object = reader.readObject();
        
        JsonArray vertices = object.getJsonArray("vertices");
        JsonArray uvs = null;
        if(!object.getJsonArray("uvs").isEmpty()){
            uvs=object.getJsonArray("uvs").getJsonArray(0);
        }
        JsonArray faces = object.getJsonArray("faces");
        JsonArray skinIndices = object.getJsonArray("skinIndices");
        JsonArray skinWeights = object.getJsonArray("skinWeights");
        JsonArray normals = object.getJsonArray("normals");
        
        if(debug){
            System.out.println("vertices = " + vertices.size());
            if(uvs!=null){
                System.out.println("uvs = " + uvs.size());
            }
            System.out.println("faces = " + faces.size());
            System.out.println("skinIndices = " + skinIndices.size());
            System.out.println("skinWeights = " + skinWeights.size());
            System.out.println("normals = " + normals.size());
        }
        
        JsonObject metadata = object.getJsonObject("metadata");
        int facesNumber = metadata.getInt("faces");
        int nPoints = metadata.getInt("vertices");
        int texCoordsNumber = 2; // so at least nTVerts=1
        if(!metadata.getJsonArray("uvs").isEmpty()){
            texCoordsNumber=metadata.getJsonArray("uvs").getInt(0);
        }
        final int MINMAXLEN = vertices.size()/nPoints; // 3
        float[] min = new float[MINMAXLEN];
        float[] max = new float[MINMAXLEN];
        Arrays.fill(min, Integer.MAX_VALUE);
        Arrays.fill(max, Integer.MIN_VALUE);
        
        final int LEN = faces.size()/facesNumber; // (texCoordsNumber>0?11:8); 
        // item 0: type of element: is always 42/34
        final int V1 = 1;
        final int V2 = 2;
        final int V3 = 3;
        // item 4: material_index: is always 0
        final int UV1 = 5;
        final int UV2 = 6;
        final int UV3 = 7;
        final int N1 = (uvs!=null)?8:5;
        final int N2 = (uvs!=null)?9:6;
        final int N3 = (uvs!=null)?10:7;
        
        int[][] pfaces = new int[facesNumber][];
        int[][] pnormals = new int[facesNumber][];
        
        PolygonMesh polygonMesh = new PolygonMesh();
        polygonMesh.getPoints().ensureCapacity(nPoints);
        polygonMesh.getTexCoords().ensureCapacity(texCoordsNumber);
        polygonMesh.getFaceSmoothingGroups().ensureCapacity(facesNumber);

        for (int i = 0; i < vertices.size(); i++) {
            float c = (float) (scale* vertices.getJsonNumber(i).doubleValue());
            polygonMesh.getPoints().addAll(c);
            int j = i % MINMAXLEN;
            min[j] = Math.min(min[j], c);
            max[j] = Math.max(max[j], c);            
        }
        if(uvs!=null){
            for (int i = 0; i < uvs.size(); i++) {
                polygonMesh.getTexCoords().addAll((float) uvs.getJsonNumber(i).doubleValue());
            }
        } else {
            for (int i = 0; i < texCoordsNumber; i++) {
                polygonMesh.getTexCoords().addAll(0f); // create at least 2 coordinates
            }
        }
        float[] fnormals = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            fnormals[i] = (float) normals.getJsonNumber(i).doubleValue();
        }
        
        for (int i = 0; i < faces.size(); i += LEN) {
            pfaces[i / LEN] = new int[] {
                faces.getInt(i + V1), (uvs!=null?faces.getInt(i + UV1):0),
                faces.getInt(i + V2), (uvs!=null?faces.getInt(i + UV2):0),
                faces.getInt(i + V3), (uvs!=null?faces.getInt(i + UV3):0)};
            pnormals[i / LEN] = new int[] { 
                faces.getInt(i + N1),faces.getInt(i + N2),faces.getInt(i + N3)};
        }
        polygonMesh.faces = pfaces;
        int[] smGroups = SmoothingGroups.calcSmoothGroups(pfaces, pnormals, fnormals);
        polygonMesh.getFaceSmoothingGroups().setAll(smGroups);
        if(debug){ 
            System.out.println("polygonMesh.getFaceSmoothingGroups() = " + polygonMesh.getFaceSmoothingGroups());
        }
        
        if(debug){
            for (int i = 0; i < MINMAXLEN; i++) {
                System.out.println(i + ", min[i] = " + min[i] + ", max[i] = " + max[i]);
            }
        }
        
        final int nJoints = metadata.getInt("bones");
        float[][] weights = new float[nJoints][nPoints];
        Affine[] bindTransforms = new Affine[nJoints];
        Affine bindGlobalTransform = new Affine();
        List<Joint> joints = new ArrayList<>(nJoints);
        
        for (int i = 0; i < nJoints; i++) {
            JsonObject bone = object.getJsonArray("bones").getJsonObject(i);
            Joint joint = new Joint();
            String name = bone.getString("name");
            if(debug){
                System.out.println("name = " + name);
            }
            joint.setId(name);
            JsonArray pos = bone.getJsonArray("pos");
            double x = scale * pos.getJsonNumber(0).doubleValue();
            double y = scale * pos.getJsonNumber(1).doubleValue();
            double z = scale * pos.getJsonNumber(2).doubleValue();
            joint.t.setX(x);
            joint.t.setY(y);
            joint.t.setZ(z);
            bindTransforms[i] = new Affine();
            int parentIndex = bone.getInt("parent");
            if (parentIndex == -1) {
                if(axes){
                    joint.getChildren().add(new Axes(0.04));
                }
                jointForest.add(joint);
                bindTransforms[i] = new Affine(new Translate(-x, -y, -z));
            } else {
                if(axes){
                    joint.getChildren().add(new Axes(0.02));
                }
                Joint parent = joints.get(parentIndex);
                parent.getChildren().add(joint);
                if(skeletal){
                    parent.getChildren().add(new Bone(0.02,new Point3D(x, y, z)));
                }
                try {
                    bindTransforms[i] = new Affine(joint.getLocalToSceneTransform().createInverse());
                } catch (NonInvertibleTransformException ex) {
                    System.out.println("Error: "+ex);
                }
            }
            joints.add(joint);
        }
        
        for (int i = 0; i < skinIndices.size(); i += 2) {
            int pIndex = i / 2;
            int jIndex1 = skinIndices.getInt(i);
            int jIndex2 = skinIndices.getInt(i + 1);
            float weight1 = (float) skinWeights.getJsonNumber(i).doubleValue();
            float weight2 = (float) skinWeights.getJsonNumber(i + 1).doubleValue();
            float total = weight1 + weight2;
            weight1 /= total;
            weight2 /= total;
            if (weights[jIndex1][pIndex] == 0) {
                weights[jIndex1][pIndex] = weight1;
            }
            if (weights[jIndex2][pIndex] == 0) {
                weights[jIndex2][pIndex] = weight2;
            }
        }
        if(debug){
            for (int j = 0; j < nPoints; j++) {
                double total = 0;
                for (int i = 0; i < nJoints; i++) {
                    double w = weights[i][j];
                    total += w;
                }
                if (Math.abs(total - 1) > 1e-3) {
                    System.out.println("j = " + j + ", total = " + total);
                    for (int i = 0; i < nJoints; i++) {
                        double w = weights[i][j];
                        if (w > 0) {
                            System.out.println("  i = " + i + ", w = " + w);
                        }
                    }
                }
            }
        }
        SkinningMesh skinningMesh = new SkinningMesh(polygonMesh, weights, 
                bindTransforms, bindGlobalTransform, joints, jointForest);
        skinningMeshView = new PolygonMeshView(skinningMesh);
        PhongMaterial phongMaterial = new PhongMaterial();
//        phongMaterial.setDiffuseMap(new Image(getClass().getResourceAsStream("skin_texture_by_rosedecastille-d4lgv9y.jpg")));
        phongMaterial.setDiffuseColor(Color.SANDYBROWN);
        skinningMeshView.setMaterial(phongMaterial);
//        skinningMeshView.setSubdivisionLevel(1); // NOT SUPPORTED FOR SKINNING MESHES
        if(skeletal){
            skinningMeshView.setDrawMode(DrawMode.LINE);
        }
        skinningMeshView.setCullFace(CullFace.BACK);
    }
    
    public PolygonMeshView getSkinningMeshView() { return skinningMeshView; }
    
    public List<Parent> getJointForest() { return jointForest; }
}
