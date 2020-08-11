package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CarrierGateway;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final AmazonEC2 client = ClientBuilder.getClient();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(describeCarrierGateway(model.getCarrierGatewayId(), proxy, client))
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ResourceModel describeCarrierGateway(
            final String cagwId,
            final AmazonWebServicesClientProxy proxy,
            final AmazonEC2 client) {

        String nextToken = null;
        CarrierGateway cagw = null;
        try {
            do {
                final DescribeCarrierGatewaysRequest request = new DescribeCarrierGatewaysRequest()
                        .withCarrierGatewayIds(cagwId)
                        .withNextToken(nextToken);
                final DescribeCarrierGatewaysResult result = proxy.injectCredentialsAndInvoke(request, client::describeCarrierGateways);
                if (result.getCarrierGateways().size() > 1) {
                    throw new CfnGeneralServiceException("Should be 1 cagw when reading, but was " + result.getCarrierGateways());
                }
                if (!result.getCarrierGateways().isEmpty()) {
                    cagw = result.getCarrierGateways().get(0);
                }
                nextToken = result.getNextToken();
            } while (cagw == null && nextToken != null);
            if (cagw == null) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, cagwId);
            }
            return createModelFromCarrierGateway(cagw);
        } catch (AmazonEC2Exception e) {
            if ("InvalidCarrierGatewayID.NotFound".equals(e.getErrorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, cagwId);
            }
            throw e;
        }
    }
}
