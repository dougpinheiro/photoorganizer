package dougpinheiro.photoorganizer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

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
	private long progress = 0;
	private JFrame mainFrame;
	private JProgressBar progressBar;

	public void start(){
		setDefaultDirectories();
		startGUI();
		calcBytesToProcess(this.sourceFile);
		
		if(this.totalBytes > this.rootDirectory.getFreeSpace()){
			JOptionPane.showMessageDialog(this.mainFrame, "The storage is full!");
			System.exit(0);
		}

		/*Set file filter*/
		ff = new FileFilter() {
			public boolean accept(File file) {
				if(file.isDirectory() || file.getName().toUpperCase().endsWith(".JPG")){
					return true;
				}
				return false;
			}
		};

		createPhotoTree(this.sourceFile);
		
		System.out.println(this.totalBytes);
		System.exit(0);
	}

	public long calcBytesToProcess(File directory){
		File[] files = directory.listFiles();
		long tmpLong = 0;
		try {
			tmpLong = Files.size(directory.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (File f : files) {
			if(!f.isDirectory()){
				try {
					this.totalBytes+=Files.size(f.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				this.totalBytes+=calcBytesToProcess(f);
			}
		}
		return tmpLong;
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
					mainFrame.hide();
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
						updateProgressBar(Files.size(f.toPath()));
						byte[] fTemp = Files.readAllBytes(fileTemp.toPath());
						byte[] fTemp2 = Files.readAllBytes(f.toPath());
						if (Arrays.equals(fTemp,fTemp2)){
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

	public static void main(String[] args) {
		PhotoOrganizer fo = new PhotoOrganizer();
		fo.start();
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

}
