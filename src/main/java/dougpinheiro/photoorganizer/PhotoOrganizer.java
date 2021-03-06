package dougpinheiro.photoorganizer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PhotoOrganizer {

	private String source;
	private File sourceFile;
	private String target;
	private File targetFile;
	private File rootDirectory;
	private SimpleDateFormat sdf;
	private FileFilter ff;
	private long totalBytes = 0;
	private int totalFiles = 0;
	private long progress = 0;
	private JFrame mainFrame;
	private JProgressBar progressBar;
	private Map<String, String> duplicate = new HashMap<String, String>();

	public void start(){
		setDefaultDirectories();
		startGUI();
		
		calcBytesToProcess(this.sourceFile);
		System.out.println("->"+this.totalBytes);

		if(this.totalBytes > this.rootDirectory.getFreeSpace()){
			JOptionPane.showMessageDialog(this.mainFrame, "The storage is full!");
			System.exit(0);
		}

		/*Set file filter*/
		/*ff = new FileFilter() {
			public boolean accept(File file) {
				if(file.isDirectory() || file.getName().toUpperCase().endsWith(".JPG")){
					return true;
				}
				return false;
			}
		};*/

		createPhotoTree(this.sourceFile);
		
		System.out.println("Calculating files to process...");
		
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		calcFilesToProcess(this.rootDirectory);
		
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Total of files to process: "+this.totalFiles+" files.");
		
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		excludeDuplicate(this.rootDirectory);
		
		System.exit(0);
	}

	public long calcBytesToProcess(File directory){
		File[] files = directory.listFiles();
		long tmpLong = 0;
		/*try {
			tmpLong = Files.size(directory.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		for (File f : files) {
			if(!f.isDirectory()){
				try {
					tmpLong = Files.size(f.toPath());
					this.totalBytes+=tmpLong;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				this.totalBytes+=calcBytesToProcess(f);
			}
		}
		return tmpLong;
	}
	
	public int calcFilesToProcess(File directory){
		File[] files = directory.listFiles();
		int tmpInt = 1;

		for (File f : files) {
			if(!f.isDirectory()){
					this.totalFiles++;
			}else{
				this.totalFiles+=calcFilesToProcess(f);
			}
		}
		return tmpInt;
	}

	public void startGUI(){
		this.mainFrame = new JFrame("Copying files...");
		this.mainFrame.setAlwaysOnTop(true);
		this.mainFrame.setResizable(false);
		this.progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		this.mainFrame.add(this.progressBar);
		this.progressBar.setVisible(true);
		this.progressBar.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				if (progressBar.getValue() == 100){
					//progressBar.setValue(0);
					progress = 0;
				}				
			}
		});
		this.mainFrame.pack();
		this.mainFrame.show();
	}
	public void createPhotoTree(File directory){
		File[] arquivos = directory.listFiles();

		for (File f : arquivos) {
			if(!f.isDirectory()){
				BasicFileAttributes bfa = null;
				try {
					Calendar createdDate;
					bfa = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
					createdDate = Calendar.getInstance();
					createdDate.setTimeInMillis(bfa.lastModifiedTime().toMillis());
					File directoryTemp = new File(this.rootDirectory.getAbsolutePath()+
							File.separator+createdDate.get(Calendar.YEAR)+
							File.separator+(createdDate.get(Calendar.MONTH)+1));
					File fileTemp = new File(this.rootDirectory.getAbsolutePath()+
							File.separator+createdDate.get(Calendar.YEAR)+
							File.separator+(createdDate.get(Calendar.MONTH)+1)+
							File.separator+f.getName());

					if(!directoryTemp.exists()){
						directoryTemp.mkdirs();
					}

					if(!fileTemp.exists()){
						fileTemp.setLastModified(createdDate.getTimeInMillis());
						FileOutputStream fos = new FileOutputStream(fileTemp);

						updateProgressBar(Files.copy(f.toPath(), fos));
						fos.flush();
						fos.close();
					}else{
						byte[] fTemp = Files.readAllBytes(fileTemp.toPath());
						byte[] fTemp2 = Files.readAllBytes(f.toPath());
						if (Arrays.equals(fTemp,fTemp2)){
							updateProgressBar(Files.size(f.toPath()));
							continue;
						}else{
							FileOutputStream fos = new FileOutputStream(fileTemp);
							updateProgressBar(Files.copy(f.toPath(), fos));
							fos.flush();
							fos.close();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				createPhotoTree(f);
			}
		}
	}

	public void updateProgressBar(long value){
		System.out.println("Updating...");
		this.progress+=value;
		double tmpLong = (this.progress * 100)/this.totalBytes;
		System.out.println(tmpLong);
		this.progressBar.setValue((int)Math.round(tmpLong));
	}
	
	public void updateProgressBar(){
		System.out.println("Updating...");
		this.progress++;
		double tmpInt = (this.progress * 100)/this.totalFiles;
		System.out.println(tmpInt);
		this.progressBar.setValue(Integer.valueOf(String.valueOf(Math.round(tmpInt))));
	}

	public void setDefaultDirectories() {
		Locale.setDefault(Locale.ENGLISH);		
		JFileChooser sourceFileChooser = new JFileChooser(new File("."));
		sourceFileChooser.setDialogTitle("Source Directory");
		sourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		sourceFileChooser.setAcceptAllFileFilterUsed(false);
		sourceFileChooser.showDialog(sourceFileChooser, "Select");
		this.source = sourceFileChooser.getSelectedFile().getAbsolutePath();
		this.sourceFile = new File(this.source);
		System.out.println("Source-> "+this.source);

		JFileChooser targetFileChooser = new JFileChooser(new File("."));
		targetFileChooser.setDialogTitle("Target Directory");
		targetFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		targetFileChooser.setAcceptAllFileFilterUsed(false);
		targetFileChooser.showDialog(targetFileChooser, "Select");
		this.target = targetFileChooser.getSelectedFile().getAbsolutePath();
		this.targetFile = new File(this.target);
		this.rootDirectory = new File(this.targetFile.getAbsolutePath()+File.separator+"MyOrganizedPhotos");
		System.out.println("Target-> "+this.target);

		if(!this.targetFile.canWrite()){
			JOptionPane.showMessageDialog(sourceFileChooser, "Invalid target directory!");
			System.exit(0);
		}

		this.rootDirectory.mkdir();
	}

	public void excludeDuplicate(File in){
		MessageDigest md;
		if(in.isDirectory()){
			for (File f : in.listFiles()) {
				excludeDuplicate(f);
			}			
		}else{
			try {
				updateProgressBar();
				md = MessageDigest.getInstance("MD5");
				String id = new String(Base64.getEncoder().encode(md.digest(Files.readAllBytes(in.toPath()))));
				System.out.println(in.getAbsolutePath()+" \t -"+id);
				if(this.duplicate.containsKey(id)){
					this.duplicate.replace(id, this.duplicate.get(id)+"->"+in.toPath().toString());
					in.delete();
				}else{
					this.duplicate.put(id, in.toPath().toString());					
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void showDuplicates(){
		for (Iterator<String> key = this.duplicate.keySet().iterator(); key.hasNext();) {
			String k = String.valueOf(key.next());
			if(this.duplicate.get(k).contains("->")){
				System.out.println(k+" \t ->"+this.duplicate.get(k));
			}
		}		
	}

	public static void main(String[] args) {
		PhotoOrganizer fo = new PhotoOrganizer();
		fo.start();
		fo.showDuplicates();
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public File getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public SimpleDateFormat getSdf() {
		return sdf;
	}

	public void setSdf(SimpleDateFormat sdf) {
		this.sdf = sdf;
	}

	public FileFilter getFf() {
		return ff;
	}

	public void setFf(FileFilter ff) {
		this.ff = ff;
	}

	public long getTotalBytes() {
		return totalBytes;
	}

	public void setTotalBytes(long totalBytes) {
		this.totalBytes = totalBytes;
	}

	public JFrame getMainFrame() {
		return mainFrame;
	}

	public void setMainFrame(JFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public void setProgressBar(JProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public long getProgress() {
		return progress;
	}

	public void setProgress(long progress) {
		this.progress = progress;
	}

	public Map<String, String> getDuplicate() {
		return duplicate;
	}

	public void setDuplicate(Map<String, String> duplicate) {
		this.duplicate = duplicate;
	}

	public int getTotalFiles() {
		return totalFiles;
	}

	public void setTotalFiles(int totalFiles) {
		this.totalFiles = totalFiles;
	}

}
