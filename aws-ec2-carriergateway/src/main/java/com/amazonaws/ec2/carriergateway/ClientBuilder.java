package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;


class ClientBuilder {
  private ClientBuilder() { }

  static AmazonEC2 getClient() {
    return AmazonEC2ClientBuilder.standard().build();
  }
}