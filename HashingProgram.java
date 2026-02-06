import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class HashingProgram {

    //hashing algorithm to be used
    private static final String ALGORITHM = "SHA3-256";
    private static final String HASH_TABLE_FILENAME = "hashtable.json";

    private static void printMessage() {
        System.out.println("Hashing Program using " + ALGORITHM);
        System.out.println("Select an option:");
        System.out.println("1. Generate Hash");
        System.out.println("2. Verify Hash");
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        printMessage();
        String choice = scanner.nextLine().trim();

        try {
            if ("1".equals(choice)) {
                System.out.println("Enter the directory path:");
                Path dirPath = Paths.get(scanner.nextLine().trim());
                generateTable(dirPath);
               
            }
            else if ("2".equals(choice)) {
                System.out.println("Enter the directory path to verify:");
                Path dirPath = Paths.get(scanner.nextLine().trim());
                validateHash(dirPath);     
                
            } else {
                System.out.println("Invalid option selected. Program will exit.");
            }
            scanner.close();
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }


        
    }
    private static void validateHash(Path dirPath) {

        // Check if hashtable.json exists
        if(!Files.exists(dirPath.resolve(HASH_TABLE_FILENAME))) {
            System.out.println("Hash table file not found in the specified directory.");
            return;
        }
        try {
            // Read the hashtable.json file
            String jsonFileContent = Files.readString(dirPath.resolve(HASH_TABLE_FILENAME), StandardCharsets.UTF_8);
            Map<String, String> existingHashMap = new HashMap<>();

            // Simple JSON parsing 
            jsonFileContent = jsonFileContent.trim().substring(1, jsonFileContent.length() - 1).trim(); 
            String[] mapValues = jsonFileContent.split(",\n");

            for (String map : mapValues) {
                String[] pair = map.split(":",2);
                String filePath = pair[0].trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\\", "\\");
                String fileHash = pair[1].trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\\", "\\")
                .replaceAll("[,}\"]$", "").replaceAll("[,}\"]$", "");
                fileHash = fileHash.trim();
                existingHashMap.put(filePath, fileHash);
            }

            // Validating each file's hash
            for (Map.Entry<String, String> map : existingHashMap.entrySet()) {
                Path filePath = Paths.get(map.getKey());
                if (!Files.exists(filePath)) {
                    System.out.println(filePath + " file Deleted ");
                    continue;
                }
                String currentHash = hashFile(filePath);

                if (currentHash.equals(map.getValue())) {
                    System.out.println(filePath + " hash is VALID");
                } 
                if(!currentHash.equals(map.getValue())) {
                    System.out.println(filePath + " hash is INVALID");
                   
                }
            }
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
                for (Path path : directoryStream) {
                    if(!Files.isRegularFile(path))  continue; // Skip non-regular files like directories
                    if(path.getFileName().toString().toLowerCase().endsWith(".json")) continue; // Skip hashed .json files
                    
                    String filePath = path.toAbsolutePath().normalize().toString();
                    if(!existingHashMap.containsKey(filePath)){
                        System.out.println(filePath + " New file Added");
                    }
                    
                }
            }

        } catch (Exception e) {
            System.out.println("Error during hash validation: " + e.getMessage());
        }

        
    }

    private static void generateTable(Path dirPath) throws IOException {
        Map<String, String> hashMap = traverseDirectory(dirPath);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int count = 0;
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {

            String filePath = entry.getKey().replace("\\", "\\\\").replace("\"", "\\\"");
            String fileHash = entry.getValue().replace("\\", "\\\\").replace("\"", "\\\"");

            sb.append("  \"").append(filePath).append("\": \"").append(fileHash).append("\"");

            if(count<hashMap.size()-1) sb.append(","); 
            sb.append("\n");
            count++;
            
        }
        sb.append("}\n");

        String jsonOutput = sb.toString();

        Files.writeString(dirPath.resolve(HASH_TABLE_FILENAME), jsonOutput, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Hash table generated and saved to " + dirPath.toAbsolutePath().normalize().toString());
    }

    // Traverse the directory and hash each file, returning a map of file paths to their hashes
    private static Map<String, String> traverseDirectory(Path dirPath) {
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("The provided path is not a directory.");
        }

        Map<String, String> hashMap = new HashMap<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
            for (Path path : directoryStream) {
                if(!Files.isRegularFile(path))  continue; // Skip non-regular files like directories
                if(path.getFileName().toString().toLowerCase().endsWith(".json")) continue; // Skip hashed .json files
                
                try{
                    String filePath = path.toAbsolutePath().normalize().toString();
                    String fileHash = hashFile(path);
                    hashMap.put(filePath, fileHash);
                }
                catch(Exception e){
                    hashMap.put(path.toAbsolutePath().normalize().toString(), "ERROR: " + e.getMessage()); // Make note of files that couldn't be hashed without crasing the whole process
                }
            }
               
            
        } catch (Exception e) {
            System.out.println("Error traversing directory: " + e.getMessage());
        }

       return hashMap;
    
    }

    // Hash a file using SHA3-256 and return the hexadecimal string representation
    private static String hashFile(Path path) {
        MessageDigest messageDigest;
        final int BUFFER_SIZE = 65536; // 64KB
        try{
            messageDigest = MessageDigest.getInstance(ALGORITHM);
            try (InputStream inputStream = Files.newInputStream(path)){
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
                
            }
            
        }
        catch(Exception e){
            throw new RuntimeException("Error hashing file: " + e.getMessage());
        }
        byte[] digestBytes = messageDigest.digest();
        StringBuilder stringBuilder = new StringBuilder(digestBytes.length * 2);
        for (byte b : digestBytes) {
             stringBuilder.append(String.format("%02x", b));
        }

        return stringBuilder.toString();
    }
    
}
