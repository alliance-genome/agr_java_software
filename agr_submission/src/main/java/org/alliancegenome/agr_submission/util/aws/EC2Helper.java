package org.alliancegenome.agr_submission.util.aws;

import org.alliancegenome.agr_submission.config.ConfigHelper;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.VolumeType;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class EC2Helper {

    public void listInstances() {
        boolean done = false;

        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials(ConfigHelper.getAWSAccessKey(), ConfigHelper.getAWSSecretKey())))
                .withRegion(Regions.US_EAST_1).build();

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        while(!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for(Reservation reservation : response.getReservations()) {
                for(Instance instance : reservation.getInstances()) {
                    System.out.printf("Instance: %s, AMI: %s, Type: %s, State: %s, Monitoring: %s, Name: %s\n",
                        instance.getInstanceId(),
                        instance.getImageId(),
                        instance.getInstanceType(),
                        instance.getState().getName(),
                        instance.getMonitoring().getState(),
                        instance.getTags().get(0).getValue()
                    );
                }
            }

            request.setNextToken(response.getNextToken());

            if(response.getNextToken() == null) {
                done = true;
            }
        }
    }

    public void createInstance() {
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(ConfigHelper.getAWSAccessKey(), ConfigHelper.getAWSSecretKey())))
                .withRegion(Regions.US_EAST_1).build();

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        EbsBlockDevice root_ebs = new EbsBlockDevice().withVolumeSize(200).withVolumeType(VolumeType.Gp2);
        EbsBlockDevice swap_ebs = new EbsBlockDevice().withVolumeSize(64).withVolumeType(VolumeType.Gp2);
        BlockDeviceMapping root = new BlockDeviceMapping().withDeviceName("/dev/xvda").withEbs(root_ebs);
        BlockDeviceMapping swap = new BlockDeviceMapping().withDeviceName("/dev/sdb").withEbs(swap_ebs);

        runInstancesRequest.withImageId("ami-0b1db01d775d666c2")
        .withInstanceType(InstanceType.R52xlarge).withMinCount(1).withMaxCount(1)
        .withBlockDeviceMappings(root, swap)
        .withSecurityGroups("default", "ES Transport", "HTTP", "HTTPS SSL", "SSH") // Step 6 default, ES Transport, HTTP, HTTPS SSL, SSH
        .withKeyName("AGR-ssl2");

        RunInstancesResult result = ec2.runInstances(runInstancesRequest);

        log.info(result);

    }

}
