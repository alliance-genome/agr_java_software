package org.alliancegenome.indexer.variant.scripts;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.data_extractor.extractors.fms.DataFile;
import org.alliancegenome.data_extractor.extractors.fms.interfaces.DataFileRESTInterface;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class TestDownloadSplitUploadMOD {

	private String fileSaveLocation = ConfigHelper.getVariantDownloadPath();

	public static void main(String[] args) throws Exception {
		new TestDownloadSplitUploadMOD();
	}

	public TestDownloadSplitUploadMOD() throws Exception {
		DataFileRESTInterface api = RestProxyFactory.createProxy(DataFileRESTInterface.class, ConfigHelper.getFMSUrl());

		List<DataFile> list = api.getDataTypeFiles("HTPOSTVEPVCF", true);

		HashSet<String> skipSet = new HashSet<String>();
		skipSet.add("FB");
		skipSet.add("RGD");
		
		for(DataFile df: list) {
			if(!skipSet.contains(df.getDataSubType().getName())) {
				File localFile = downloadFile(df);
	
				List<File> uploadList = splitFile(localFile, df);
	
				uploadFiles(uploadList, df);
	
				log.info("Finished: " + df.getS3Url());
			}
		}

	}

	private File downloadFile(DataFile df) throws Exception {
		try {
			URL url = new URL(df.getS3Url());
			File s3Url = new File(df.getS3Url());

			File saveLocation = new File(fileSaveLocation + "/" + s3Url.getName());
			if(!saveLocation.exists()) {
				log.info("Downloading: " + saveLocation);
				FileUtils.copyURLToFile(url, saveLocation);
			} else {
				log.info("File already exists: " + saveLocation);
			}
			log.info("Finished Downloading: " + saveLocation);
			return saveLocation;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public List<File> splitFile(File localFile, DataFile df) {

		ArrayList<File> ret = new ArrayList<>();

		VCFFileReader reader = new VCFFileReader(localFile, false);

		String chr = "";

		VariantContextWriter writer = null;

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

		CloseableIterator<VariantContext> iter1 = reader.iterator();

		ph.startProcess("VCFReader Reader: ");

		while(iter1.hasNext()) {
			try {
				VariantContext vc = iter1.next();
				if(!vc.getChr().equals(chr)) {
					if(writer != null) writer.close();
					chr = vc.getChr();
					VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
					String chrFile = fileSaveLocation + "/" + df.getDataSubType().getName() + ".vep." + chr + ".vcf.gz";
					String tbiFile = fileSaveLocation + "/" + df.getDataSubType().getName() + ".vep." + chr + ".vcf.gz.tbi";
					log.info("Opening new File: " + chrFile);
					ret.add(new File(chrFile));
					ret.add(new File(tbiFile));
					builder.setOutputFile(chrFile);
					writer = builder.build();
					writer.writeHeader(reader.getFileHeader());
				}
				writer.add(vc);
				ph.progressProcess();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		writer.close();
		ph.finishProcess();

		return ret;
	}

	public void uploadFiles(List<File> uploadFiles, DataFile df) {
		AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ConfigHelper.loadSystemENVProperty("AWS_ACCESS_KEY"), ConfigHelper.loadSystemENVProperty("AWS_SECRET_KEY")))).withRegion(Regions.US_EAST_1).build();

		TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();
		

		for(File f: uploadFiles) {
			String s3Path = "variants/" + df.getDataSubType().getName() + "/" + f.getName();
			
			log.info("Uploading file to S3: " + f.getAbsolutePath() + " -> s3://" + ConfigHelper.getAWSBucketName() + "/variants/" + df.getDataSubType().getName() + "/" + f.getName());
			
			final Upload uploadFile = tm.upload(ConfigHelper.getAWSBucketName(), s3Path, f);
			try {
				uploadFile.waitForCompletion();
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			log.info("Upload Finished: " + f.getAbsolutePath());
		}
		
		tm.shutdownNow();
		log.info("S3 Upload complete");
		s3.shutdown();
	}

}
