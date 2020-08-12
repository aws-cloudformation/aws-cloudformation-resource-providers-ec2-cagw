package com.amazonaws.ec2.carriergateway;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.carriergateway.Translator.getHandlerErrorForEc2Error;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        final AmazonEC2 client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();

        if (previousModel.getCarrierGatewayId() == null) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .message("CarrierGateway not found")
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .build();
        }

        if (model.getCarrierGatewayId() != null && !model.getCarrierGatewayId().equals(previousModel.getCarrierGatewayId())) {
            return createNotUpdatableEvent(model, "CarrierGatewayId");
        }

        if (model.getOwnerId() != null && !model.getOwnerId().equals(previousModel.getOwnerId())) {
            return createNotUpdatableEvent(model, "OwnerId");
        }

        if (model.getState() != null && !model.getState().equals(previousModel.getState())) {
            return createNotUpdatableEvent(model, "State");
        }

        if (callbackContext == null || !callbackContext.isUpdateStarted()) {
            final ReadHandler readHandler = new ReadHandler();
            final ResourceModel existingResource;
            try {
                existingResource = readHandler.handleRequest(proxy, request, callbackContext, logger).getResourceModel();
            } catch (AmazonEC2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                        .message(e.getMessage())
                        .build();
            }
            if (!existingResource.getVpcId().equals(model.getVpcId())) {
                return createNotUpdatableEvent(model, "VpcId");
            }
            final Set<Tag> currentTags = existingResource.getTags();
            final Set<Tag> desiredTags = model.getTags();
            if (currentTags.equals(desiredTags)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build();
            }

            // To make this update minimally intrusive we only change tags that are not staying the same between updates
            final Set<Tag> tagsToCreate = desiredTags
                    .stream()
                    .filter(tag -> !currentTags.contains(tag))
                    .collect(Collectors.toSet());
            final Set<Tag> tagsToDelete = currentTags
                    .stream()
                    .filter(tag -> !desiredTags.contains(tag))
                    .collect(Collectors.toSet());

            final CallbackContext nextContext = CallbackContext
                    .builder()
                    .updateStarted(true)
                    .tagsToCreate(tagsToCreate)
                    .tagsToDelete(tagsToDelete)
                    .build();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(nextContext)
                    .resourceModel(model)
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        }

        // Create new tags before deleting old ones
        try {
            if (callbackContext.getTagsToCreate() != null && !callbackContext.getTagsToCreate().isEmpty()) {
                final CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                        .withTags(callbackContext.getTagsToCreate().stream().map(Translator::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                        .withResources(model.getCarrierGatewayId());
                proxy.injectCredentialsAndInvoke(createTagsRequest, client::createTags);
            }
            if (callbackContext.getTagsToDelete() != null && !callbackContext.getTagsToDelete().isEmpty()) {
                final DeleteTagsRequest deleteTagsRequest = new DeleteTagsRequest()
                        .withTags(callbackContext.getTagsToDelete().stream().map(Translator::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                        .withResources(model.getCarrierGatewayId());
                proxy.injectCredentialsAndInvoke(deleteTagsRequest, client::deleteTags);
            }
        } catch (AmazonEC2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.getErrorCode()))
                    .message(e.getMessage())
                    .build();
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> createNotUpdatableEvent(ResourceModel model, String nonUpdatableProperty) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .message("Cannot update not updatable property " + nonUpdatableProperty)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotUpdatable)
                .build();
    }
}
