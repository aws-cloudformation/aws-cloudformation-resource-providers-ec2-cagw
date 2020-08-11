package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateCarrierGatewayRequest;
import com.amazonaws.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.carriergateway.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;
import static com.amazonaws.ec2.carriergateway.Translator.getHandlerErrorForEc2Error;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        Set<Tag> tags = model.getTags();
        final AmazonEC2 client = ClientBuilder.getClient();
        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            // Return InvalidRequest if caller is attempting to set a read-only property
            if (model.getCarrierGatewayId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "CarrierGatewayId");
            }
            if (model.getOwnerId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "OwnerId");
            }
            if (model.getState() != null) {
                return createFailedReadOnlyPropertyEvent(model, "State");
            }
            try {
                model = createCarrierGateway(model.getVpcId(), tags, proxy, client);
            } catch (AmazonEC2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                        .message(e.getMessage())
                        .build();
            }
            if (tags != null) {
                model.setTags(tags);
            }
            return createInProgressEvent(model, 0);
        }

        final ReadHandler readHandler = new ReadHandler();
        final ResourceModel resultModel;
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            if (!"available".equals(resultModel.getState())) {
                return createInProgressEvent(resultModel);
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEvent(model);
        } catch (AmazonEC2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                    .message(e.getMessage())
                    .build();
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(resultModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ResourceModel createCarrierGateway(
            final String vpcId,
            final Set<Tag> tags,
            final AmazonWebServicesClientProxy proxy,
            final AmazonEC2 client) {

        final CreateCarrierGatewayRequest request = new CreateCarrierGatewayRequest().withVpcId(vpcId);

        if (tags != null && !tags.isEmpty()) {
            request.withTagSpecifications(new TagSpecification()
                    .withResourceType("carrier-gateway")
                    .withTags(tags.stream().map(Translator::createSdkTagFromCfnTag).collect(Collectors.toSet())));
        }
        try {
            return createModelFromCarrierGateway(proxy.injectCredentialsAndInvoke(request, client::createCarrierGateway)
                    .getCarrierGateway());
        } catch (AmazonEC2Exception e) {
            if ("CarrierGatewayAlreadyExists".equals(e.getErrorCode())) {
                throw new CfnAlreadyExistsException("CarrierGateway", "vpcId: " + vpcId);
            }
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        return createInProgressEvent(model, POLLING_DELAY_SECONDS);
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model, int callbackDelay) {
        CallbackContext context = CallbackContext.builder()
                .createStarted(true)
                .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(context)
                .callbackDelaySeconds(callbackDelay)
                .status(OperationStatus.IN_PROGRESS)
                .resourceModel(model)
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> createFailedReadOnlyPropertyEvent(ResourceModel model, String readOnlyProperty) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message("Cannot set read-only property " + readOnlyProperty)
                .build();
    }
}
