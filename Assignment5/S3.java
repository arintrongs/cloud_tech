import java.io.IOException;
import java.io.File;
import java.util.*;
// add other aws imports
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.SdkClientException;
import com.amazonaws.AmazonServiceException;

/**
 *
 * @author kunwadee
 */
public class S3 {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {

        /*
         * This is the basic way. But your source code will contain your key. Secure? No
         * way! So, we won't use this.
         * 
         * BasicAWSCredentials awsCreds = new BasicAWSCredentials("__yourAccessKeyId__",
         * "__yourSecretAccessKey__"); AmazonS3 s3Client =
         * AmazonS3ClientBuilder.standard() .withCredentials(new
         * AWSStaticCredentialsProvider(awsCreds)) .build();
         */

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider()).build();

        String bucketName = "__someBucketName__";
        createBucket(s3Client, bucketName);
        viewObjectsInBucket(s3Client, bucketName);
        addObjectToBucket (s3Client, bucketName, "key","__pathToFileOnYourComputer__");
        viewObjectsInBucket(s3Client, bucketName);
        // deleteBucket(s3Client, bucketName);
        System.exit(0);
    }

    // Create bucket named bucketName if it does not yet exist.
    // Catch all exceptions, and print error to stdout (System.out)
    private static void createBucket(AmazonS3 s3Client, String bucketName) {
        System.out.printf("Create Bucket => Name : %s\n",bucketName);
        try {
            // Check if bucket exists, and if does not exist create new bucket named
            // bucketName in S3
            if (!s3Client.doesBucketExistV2(bucketName)) {
                s3Client.createBucket(bucketName);
                System.out.println("Success");
            }else{
                System.out.println("Duplicate bucket name");
            }
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Study Exceptions thrown, decode and print error to stdout
            e.printStackTrace();
        }
    }

    // Add the object in filePath on my computer to the bucketName bucket on S3,
    // using the key keyName for the object
    // Catch all exceptions, and print error to stdout (System.out)
    public static void addObjectToBucket(AmazonS3 s3Client, String bucketName, String keyName, String filePath) {
        System.out.println("Add an Object");
        TransferManager tm = new TransferManager(s3Client);
        // Use TransferManager to upload file to S3
        try {
            File myFile = new File(filePath);
            Upload myUpload = tm.upload(bucketName, keyName, myFile);
            // Block and wait for the upload to finish
            myUpload.waitForCompletion();
            System.out.println("Success");
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Study Exceptions thrown, decode and print error to stdout
            e.printStackTrace();
        } catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    // List all objects in the bucketName bucket
    // Catch all exceptions, and print error to stdout (System.out)
    public static void viewObjectsInBucket(AmazonS3 s3Client, String bucketName) {
        System.out.println("View Objects");
        try {
            ListObjectsV2Result list = s3Client.listObjectsV2(bucketName);
            for (S3ObjectSummary objectSummary : list.getObjectSummaries()){
                System.out.printf(" - %s (size: %d)\n", objectSummary.getKey(), objectSummary.getSize());
            }
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        } catch (AmazonClientException e) {
            // Study Exceptions thrown, decode and print error to stdout
            e.printStackTrace();
        }

    }

    // Delete all objects and versions in the bucketName bucket, and then
    // delete the bucket itself.
    // Catch all exceptions, and print error to stdout (System.out)
    public static void deleteBucket(AmazonS3 s3Client, String bucketName) {
        System.out.printf("Delete Bucket => name : %s\n",bucketName);
        try {
            ArrayList<KeyVersion> keys = new ArrayList<KeyVersion>();
            ListObjectsV2Result list = s3Client.listObjectsV2(bucketName);
            for (S3ObjectSummary objectSummary : list.getObjectSummaries()) {
                keys.add(new KeyVersion(objectSummary.getKey()));
            }
            DeleteObjectsRequest req = new DeleteObjectsRequest(bucketName).withKeys(keys);
            s3Client.deleteObjects(req);
            s3Client.deleteBucket(bucketName);
            System.out.println("Success");
        } catch (MultiObjectDeleteException e) {
            e.printStackTrace();
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Study Exceptions thrown, decode and print error to stdout
            e.printStackTrace();
        }

    }

}
