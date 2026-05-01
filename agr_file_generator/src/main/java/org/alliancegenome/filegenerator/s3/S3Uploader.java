package org.alliancegenome.filegenerator.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.alliancegenome.core.config.ConfigHelper;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import lombok.extern.slf4j.Slf4j;

/**
 * Uploads everything in {@code GENERATED_FILES_FOLDER} to
 * {@code s3://<AWS_BUCKET_NAME>/<ALLIANCE_RELEASE>/downloads/<filename>} after all generators finish.
 *
 * Credentials chain (mirrors agr_chipmunk's S3Helper):
 *   1. AWS_PROFILE env var      -> ProfileCredentialsProvider(profile)
 *   2. AWS_ACCESS_KEY_ID + ...  -> static credentials
 *   3. EC2 instance profile     -> InstanceProfileCredentialsProvider(false)
 *   4. DefaultAWSCredentialsProviderChain (env, system props, ~/.aws, container, ...)
 *
 * Bucket name and release version are read via {@link ConfigHelper}.
 */
@Slf4j
public class S3Uploader {

	private S3Uploader() {
	}

	/**
	 * Resolves the credential provider for S3 uploads. Returns null if nothing is configured —
	 * callers should treat that as "skip the upload" rather than failing the whole run.
	 */
	public static AWSCredentialsProvider getCredentials() {
		String profile = ConfigHelper.getAwsProfile();
		if (profile != null && !profile.isEmpty()) {
			log.info("S3 credentials: AWS_PROFILE={}", profile);
			return new ProfileCredentialsProvider(profile);
		}
		String accessKey = ConfigHelper.getAwsAccessKeyId();
		String secretKey = ConfigHelper.getAwsSecretAccessKey();
		if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
			log.info("S3 credentials: AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY");
			return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
		}
		try {
			InstanceProfileCredentialsProvider inst = new InstanceProfileCredentialsProvider(false);
			inst.getCredentials(); // probe — throws if not on EC2
			log.info("S3 credentials: EC2 instance profile");
			return inst;
		} catch (Exception e) {
			// No profile, no keys, not on EC2 — give up cleanly. Callers will skip the upload.
			return null;
		}
	}

	/**
	 * Upload every regular file in {@code dir} to {@code s3://<bucket>/<release>/downloads/<name>}.
	 * Skips silently with a warn log if {@code ALLIANCE_RELEASE} isn't set.
	 */
	public static void uploadDirectory(Path dir) throws IOException {
		String release = ConfigHelper.getAllianceRelease();
		if (release == null || release.isEmpty() || "0.0.0".equals(release)) {
			log.warn("ALLIANCE_RELEASE not set — skipping S3 upload");
			return;
		}
		if (!Files.isDirectory(dir)) {
			log.warn("Generated files folder not found, skipping S3 upload: {}", dir);
			return;
		}
		AWSCredentialsProvider creds = getCredentials();
		if (creds == null) {
			log.warn("No AWS credentials available (no AWS_PROFILE, no access keys, not on EC2) — skipping S3 upload");
			return;
		}
		String bucket = ConfigHelper.getAWSBucketName();
		String keyPrefix = release + "/downloads/";

		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
				.withCredentials(creds)
				.withRegion(Regions.US_EAST_1)
				.build();
		TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();
		try (Stream<Path> stream = Files.list(dir)) {
			int uploaded = 0;
			long totalBytes = 0;
			long start = System.currentTimeMillis();
			for (Path file : (Iterable<Path>) stream::iterator) {
				if (!Files.isRegularFile(file)) {
					continue;
				}
				String key = keyPrefix + file.getFileName().toString();
				File f = file.toFile();
				log.info("Uploading {} ({} bytes) -> s3://{}/{} (GLACIER_IR)", file.getFileName(), f.length(), bucket, key);
				try {
					PutObjectRequest req = new PutObjectRequest(bucket, key, f)
							.withStorageClass(StorageClass.GlacierInstantRetrieval);
					Upload upload = tm.upload(req);
					upload.waitForCompletion();
					uploaded++;
					totalBytes += f.length();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted during S3 upload of " + file, e);
				}
			}
			log.info("S3 upload complete: {} files, {} bytes in {} ms",
					uploaded, totalBytes, System.currentTimeMillis() - start);
		} finally {
			tm.shutdownNow();
			s3.shutdown();
		}
	}
}
