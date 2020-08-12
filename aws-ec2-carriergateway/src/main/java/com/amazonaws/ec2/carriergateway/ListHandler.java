package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CarrierGateway;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ListHandler extends BaseHandler<CallbackContext> {


    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final List<ResourceModel> models = describeAllCarrierGateways(proxy, ClientBuilder.getClient());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private List<ResourceModel> describeAllCarrierGateways(
            final AmazonWebServicesClientProxy proxy,
            final AmazonEC2 client) {

        List<CarrierGateway> cagws = new ArrayList<>();
        String nextToken = null;
        do {
            final DescribeCarrierGatewaysRequest request = new DescribeCarrierGatewaysRequest()
                    .withNextToken(nextToken);
            final DescribeCarrierGatewaysResult result = proxy.injectCredentialsAndInvoke(request, client::describeCarrierGateways);
            nextToken = result.getNextToken();
            cagws.addAll(result.getCarrierGateways());
        } while (nextToken != null);
        return cagws
                .stream()
                .map(Translator::createModelFromCarrierGateway)
                .collect(toList());
    }
}
