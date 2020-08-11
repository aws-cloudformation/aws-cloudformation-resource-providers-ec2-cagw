package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.model.CarrierGateway;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  private Translator() {
  }

  static ResourceModel createModelFromCarrierGateway(final CarrierGateway cagw) {
    return ResourceModel.builder()
            .carrierGatewayId(cagw.getCarrierGatewayId())
            .vpcId(cagw.getVpcId())
            .ownerId(cagw.getOwnerId())
            .state(cagw.getState())
            .tags(cagw.getTags()
                    .stream()
                    .map(Translator::createCfnTagFromSdkTag)
                    .collect(Collectors.toSet()))
            .build();
  }

  static com.amazonaws.services.ec2.model.Tag createSdkTagFromCfnTag(final Tag tag) {
    return new com.amazonaws.services.ec2.model.Tag()
            .withKey(tag.getKey())
            .withValue(tag.getValue());
  }

  static Tag createCfnTagFromSdkTag(final com.amazonaws.services.ec2.model.Tag tag) {
    return Tag.builder()
            .key(tag.getKey())
            .value(tag.getValue())
            .build();
  }

  static HandlerErrorCode getHandlerErrorForEc2Error(final String errorCode) {
    switch (errorCode) {
      case "UnauthorizedOperation":
        return HandlerErrorCode.AccessDenied;
      case "InvalidParameter":
        return HandlerErrorCode.InvalidRequest;
      default:
        return HandlerErrorCode.GeneralServiceException;
    }
  }
}
