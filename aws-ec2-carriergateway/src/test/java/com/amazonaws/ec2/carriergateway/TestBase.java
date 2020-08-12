package com.amazonaws.ec2.carriergateway;


import com.amazonaws.services.ec2.model.CarrierGateway;
import com.amazonaws.services.ec2.model.Tag;

import java.util.Collections;

public class TestBase {
    final String CAGW_ID = "cagw-12345678912345678";
    final String VPC_ID = "vpc-12345678912345678";
    final String OWNER_ID = "123456789012";
    final CarrierGateway TEST_CAGW = new CarrierGateway()
            .withCarrierGatewayId(CAGW_ID)
            .withVpcId(VPC_ID)
            .withOwnerId(OWNER_ID)
            .withState("available")
            .withTags(Collections.emptyList());
    final CarrierGateway TEST_CAGW_WITH_TAGS = new CarrierGateway()
            .withCarrierGatewayId(CAGW_ID)
            .withVpcId(VPC_ID)
            .withOwnerId(OWNER_ID)
            .withState("available")
            .withTags(
                    new Tag().withKey("Name").withValue("MyCagw"),
                    new Tag().withKey("Stage").withValue("Prod")
            );
    final CarrierGateway PENDING_CAGW = new CarrierGateway()
            .withCarrierGatewayId(CAGW_ID)
            .withVpcId(VPC_ID)
            .withOwnerId(OWNER_ID)
            .withState("pending")
            .withTags(Collections.emptyList());
}