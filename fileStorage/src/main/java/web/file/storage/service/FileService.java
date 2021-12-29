package web.file.storage.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import web.file.storage.model.FileInfo;


@Service
public class FileService {

	public static final String ROOT_DIRECTORY = "uploads";
	public static  Path root = Paths.get(ROOT_DIRECTORY);

	public void init() {
		try {

			File dir = new File(ROOT_DIRECTORY);
			if (!dir.exists()) {
				Files.createDirectory(root);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	public void createFolder(String directory) {
		try {

			init();

			File dir = new File(ROOT_DIRECTORY+"/"+directory);

			if (!dir.exists()) {

				root = Paths.get(ROOT_DIRECTORY+"/"+directory);

				Files.createDirectory(root);
			}else {
				root  = Paths.get(ROOT_DIRECTORY+"/"+directory);
			}

		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder "+directory);
		}
	}

	public void save(String directory, MultipartFile file) {
		try {
			createFolder(directory);
			Files.copy(file.getInputStream(), root.resolve(file.getOriginalFilename()),StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			System.out.print("Could not store the file. Error: " + e.getMessage());
		}
	}


	public Resource load(String filename) {

		Resource resource= null;
		try {
			Path file = root.resolve(filename);
			resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				//throw new RuntimeException("Could not read the file!");
				return resource;
			}
		} catch (MalformedURLException e) {

			return resource;
			//throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param controller web controller
	 * @param fileName
	 * @return FileInfo
	 */
	
	public  List<FileInfo> getFile(Object controller,String fileName)
	{
		List<Path> result = new ArrayList<Path>();
		FileInfo fileInfo = new FileInfo();
	
		try {

			if (!Files.isDirectory(root)) {
				throw new IllegalArgumentException("Path must be a directory!");
			}

			// walk file tree, no more recursive loop
			try (Stream<Path> walk = Files.walk(root)) {
				result = walk
						.filter(Files::isReadable)      // read permission
						.filter(Files::isRegularFile)   // is a file
						.filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
						.collect(Collectors.toList());
				for( Path path: result) {
					
					String file = path.getFileName().toString();
					String url = MvcUriComponentsBuilder.fromMethodName(
							controller.getClass(), 
							"getFile", path.getFileName().toString()).build().toString();
					
					fileInfo = new FileInfo(file, url);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		

		List<FileInfo> fileInfos = new ArrayList<FileInfo>();
		fileInfos.add(fileInfo);
		return fileInfos;

	}
	
	/**
	 * 
	 * @param controller web controller
	 * @param directory
	 * @return FileInfo
	 */
	public List<FileInfo> getFiles(Object controller, String directory) {
		
		createFolder(directory);
		try {
			return Files.walk(root, 1).filter(path -> !path.equals(root)).map(root::relativize).map(path -> {
				String filename = path.getFileName().toString();
				String url = MvcUriComponentsBuilder.fromMethodName(
						controller.getClass(), 
						"getFile", path.getFileName().toString()).build().toString();

				return new FileInfo(filename, url);
			}).collect(Collectors.toList());

		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}

	/**
	 * 
	 * @param directory
	 */
	public void deleteAll(String directory) {
		
		createFolder(directory);
		FileSystemUtils.deleteRecursively(root.toFile());
	}
	
	/**
	 * 
	 * @param directory
	 * @param filename
	 */
	public void delete(String directory, String filename) {
		
		File dir = new File(ROOT_DIRECTORY+"/"+directory);
		File[] files = dir.listFiles();
		
		for (File f : files)
		{
		    if (f.getName().equals(filename))
		    {
		      f.delete();
		    }
		}
	}

}
