package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DeleteCarrierGatewayRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.ec2.carriergateway.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.carriergateway.Translator.getHandlerErrorForEc2Error;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();
        final AmazonEC2 client = ClientBuilder.getClient();

        if (callbackContext == null || !callbackContext.isDeleteStarted()) {
            try {
                deleteCarrierGateway(model.getCarrierGatewayId(), proxy, client);
            } catch (AmazonEC2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                        .message(e.getMessage())
                        .build();
            }
        }
        final ReadHandler readHandler = new ReadHandler();
        try {
            final ResourceModel readModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            return createInProgressEvent(readModel);
        } catch (CfnNotFoundException expected) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (AmazonEC2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                    .message(e.getMessage())
                    .build();
        }
    }

    private void deleteCarrierGateway(
            final String cagwId,
            AmazonWebServicesClientProxy proxy,
            AmazonEC2 client) {

        final DeleteCarrierGatewayRequest deleteRequest = new DeleteCarrierGatewayRequest()
                .withCarrierGatewayId(cagwId);

        try {
            proxy.injectCredentialsAndInvoke(deleteRequest, client::deleteCarrierGateway);
        } catch (AmazonEC2Exception e) {
            if ("InvalidCarrierGatewayID.NotFound".equals(e.getErrorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, cagwId);
            }
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        CallbackContext context = CallbackContext.builder()
                .deleteStarted(true)
                .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(context)
                .callbackDelaySeconds(POLLING_DELAY_SECONDS)
                .status(OperationStatus.IN_PROGRESS)
                .resourceModel(model)
                .build();
    }
}
