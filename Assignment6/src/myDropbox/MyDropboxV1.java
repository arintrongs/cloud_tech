package myDropbox;

import java.util.Scanner;
import java.io.File;
//AWS Default
import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.*;
//DynamoDB Import
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
//S3
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.model.*;

public class MyDropboxV1 {
	private String Username = "";
	  private String Password = "";
	  private Boolean isLoggedIn = false;
	  private String bucketName = "10754dropbox";

	  // AWS Services Client
	  private AmazonDynamoDB DynamoDBClient = AmazonDynamoDBClientBuilder.standard()
	      .withCredentials(new ProfileCredentialsProvider()).build();
	  private AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider())
	      .build();
	  private DynamoDBMapper mapper = new DynamoDBMapper(this.DynamoDBClient);

	  public static void main(String[] args) {

	    MyDropboxV1 myDropBox = new MyDropboxV1();
	    MyDropboxClient client = new MyDropboxClient(myDropBox);

	    Scanner scan = new Scanner(System.in);

	    System.out.println("------------------------------------------------------------------");
	    System.out.println("Welcome to MyDropboxV1!!!!");
	    System.out.println("------------------------------------------------------------------");
	    System.out.println("Commands : newuser, login, logout, put, view, get, quit");
	    System.out.println("------------------------------------------------------------------");

	    while (true) {

	      System.out.print(">> ");
	      String[] command = scan.nextLine().split(" ");

	      if (command[0].equals("newuser")) {
	        client.newUser(command);
	      }

	      if (command[0].equals("login")) {
	        client.login(command);
	      }

	      if (command[0].equals("logout")) {
	        client.logout(command);
	      }

	      if (command[0].equals("put")) {
	        client.putObject(command);
	      }

	      if (command[0].equals("view")) {
	        client.listObjects();
	      }
	      if (command[0].equals("get")) {
	        client.getObject(command);
	      }
	      if (command[0].equals("quit")) {
	        System.out.println("------------------------------------------------------------------");
	        System.out.println("Thank you for using myDropBoxV1");
	        System.out.println("See ya!!!");
	        System.out.println("------------------------------------------------------------------");
	        scan.close();
	        return;
	      }
	    }
	    
	  }

	  // Client Class
	  public static class MyDropboxClient {
		  
		private MyDropboxV1 myDropBox;

		public MyDropboxClient(MyDropboxV1 myDropBox) {
			this.myDropBox = myDropBox;
			
		}
		
		// Create new user
	    public void newUser(String[] command) {
	      try {
	        String username = command[1];
	        String password = command[2];
	        String repeat_pass = command[3];
	        if (!password.equals(repeat_pass)) {
	          System.out.println("Please repeat the correct password!");
	          return;
	        }
	        DynamoDBMapper mapper = myDropBox.getMapper();
	        User retrivedUser = mapper.load(User.class, username);
	        if (retrivedUser == null) {
	          User newuser = new User();
	          newuser.setUsername(username);
	          newuser.setPassword(password);
	          mapper.save(newuser);
	          System.out.println("New user has created successfully!!");
	        } else {
	          System.out.println("This username does existed");
	          return;
	        }

	      } catch (ArrayIndexOutOfBoundsException e) {
	        System.out.println("Please fill in the correct input!");
	      } catch (Exception e) {
	        e.printStackTrace();
	      }
	    }

	    // Do login
	    public void login(String[] command) {
	      if (myDropBox.isLoggedIn()) {
	        System.out.println("You're already logged in!!");
	        return;
	      }
	      try {
	        String username = command[1];
	        String password = command[2];

	        DynamoDBMapper mapper = myDropBox.getMapper();
	        User retrivedUser = mapper.load(User.class, username);
	        if (retrivedUser == null) {
	          System.out.println("This username doesn't exist!!");
	          return;
	        }
	        if (!retrivedUser.getPassword().equals(password)) {
	          System.out.println("Wrong password!!");
	        } else {
	          myDropBox.setLoginFlag(true);
	          myDropBox.setUsername(username);
	          myDropBox.setPassword(password);
	          System.out.println("Login Successfully!!");
	        }
	      } catch (ArrayIndexOutOfBoundsException e) {
	        System.out.println("Please fill in the correct input!");
	      }
	    }

	    // Do logout
	    public void logout(String[] command) {
	      if (!myDropBox.isLoggedIn()) {
	        System.out.println("You're not logged in!!!");
	        return;
	      }
	      myDropBox.setLoginFlag(false);
	      myDropBox.setUsername("");
	      myDropBox.setPassword("");
	      System.out.println("Logout Successfully!!!");
	    }

	    // Upload object to S3 base on each user
	    public void putObject(String[] command) {
	      if (!myDropBox.isLoggedIn()) {
	        System.out.println("You should login before using this function!!");
	        return;
	      }

	      try {

	        AmazonS3 s3Client = myDropBox.getAmazonS3Client();
	        String bucketName = myDropBox.getBucketName();
	        String keyName = myDropBox.getUsername() + "/" + command[1];
	        String filePath = command[2];

	        @SuppressWarnings("deprecation")
			TransferManager tm = new TransferManager(s3Client);

	        System.out.println("Adding an Object");

	        File myFile = new File(filePath);
	        Upload myUpload = tm.upload(bucketName, keyName, myFile);

	        myUpload.waitForCompletion();
	        System.out.println("Success");
	      } catch (AmazonServiceException e) {
	        e.printStackTrace();
	      } catch (SdkClientException e) {
	        e.printStackTrace();
	      } catch (InterruptedException e) {
	        e.printStackTrace();
	      } catch (ArrayIndexOutOfBoundsException e) {
	        System.out.println("Please fill in the correct input!");
	      }

	    }

	    // List user's objects
	    public void listObjects() {
	      if (!myDropBox.isLoggedIn()) {
	        System.out.println("You should login before using this function!!");
	        return;
	      }
	      try {
	        AmazonS3 s3Client = myDropBox.getAmazonS3Client();
	        String bucketName = myDropBox.getBucketName();
	        String username = myDropBox.getUsername();

	        ListObjectsV2Request listObjectRequest = new ListObjectsV2Request().withBucketName(bucketName)
	            .withPrefix(username);
	        ListObjectsV2Result list = s3Client.listObjectsV2(listObjectRequest);
	        for (S3ObjectSummary objectSummary : list.getObjectSummaries()) {
	          System.out.printf(" - %s\t(size: %d)\t(last modified: %s)\n",
	              objectSummary.getKey().replace(username + "/", ""), objectSummary.getSize(),
	              objectSummary.getLastModified());
	        }
	      } catch (AmazonServiceException e) {
	        e.printStackTrace();
	      } catch (AmazonClientException e) {
	        // Study Exceptions thrown, decode and print error to stdout
	        e.printStackTrace();
	      }

	    }

	    // Download selected user's object.
	    public void getObject(String[] command) {
	      if (!myDropBox.isLoggedIn()) {
	        System.out.println("You should login before using this function!!");
	        return;
	      }
	      ObjectMetadata fullObject = null;
	      try {

	        AmazonS3 s3Client = myDropBox.getAmazonS3Client();
	        String bucketName = myDropBox.getBucketName();
	        String key = myDropBox.getUsername() + "/" + command[1];

	        System.out.println("Downloading an object");
	        fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key),new File("./"+command[1]));
	        if(fullObject!=null)
	          System.out.println("Download Successfully");
	        else
	          System.out.println("File not Found!");
	      } catch (AmazonServiceException e) {
	        e.printStackTrace();
	      } catch (SdkClientException e) {
	        e.printStackTrace();
	      } catch (ArrayIndexOutOfBoundsException e) {
	        System.out.println("Please fill in the correct input!");
	      }
	    }
	  }

	  
	  // High-Level API to map attributes in table to Object Class
	  @DynamoDBTable(tableName = "myDropboxUsers")
	  public static class User {
	    private String username;
	    private String password;

	    // Partition key
	    @DynamoDBHashKey(attributeName = "username")
	    public String getUsername() {
	      return this.username;
	    }

	    public void setUsername(String username) {
	      this.username = username;
	    }

	    @DynamoDBAttribute(attributeName = "password")
	    public String getPassword() {
	      return this.password;
	    }

	    public void setPassword(String password) {
	      this.password = password;
	    }

	    @Override
	    public String toString() {
	      return "Username =" + username + ", Passowrd=" + password;
	    }
	  }

	  public AmazonDynamoDB getAmazonDynamoDBClient() {
	    return this.DynamoDBClient;
	  }

	  public AmazonS3 getAmazonS3Client() {
	    return this.s3Client;
	  }

	  public DynamoDBMapper getMapper() {
	    return this.mapper;
	  }

	  public void setUsername(String username) {
	    this.Username = username;
	  }

	  public String getUsername() {
	    return this.Username;
	  }

	  public void setPassword(String password) {
	    this.Password = password;
	  }

	  public String getPassword() {
	    return this.Password;
	  }

	  public Boolean isLoggedIn() {
	    return this.isLoggedIn;
	  }

	  public void setLoginFlag(Boolean bool) {
	    this.isLoggedIn = bool;
	  }

	  public String getBucketName() {
	    return this.bucketName;
	  }

}
