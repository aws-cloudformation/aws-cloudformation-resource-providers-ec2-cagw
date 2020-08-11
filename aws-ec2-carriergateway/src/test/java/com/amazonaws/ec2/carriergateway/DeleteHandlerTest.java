package com.amazonaws.ec2.carriergateway;

import java.util.Collections;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DeleteCarrierGatewayRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import org.mockito.Mockito;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.amazonaws.ec2.carriergateway.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends TestBase {


    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceModel model = createModelFromCarrierGateway(TEST_CAGW);

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    private final CallbackContext inProgressContext = CallbackContext.builder()
            .deleteStarted(true)
            .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_DeleteNotStarted_Success() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvoke(any(DeleteCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteNotStarted_Failed() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        final AmazonEC2Exception unexpectedException = new AmazonEC2Exception("");
        unexpectedException.setErrorCode("UnexpectedError");


        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);
        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DeleteCarrierGatewayRequest.class), any()))
                .thenThrow(unexpectedException);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_NotFound() {

        final AmazonEC2Exception notFoundException = new AmazonEC2Exception("");
        notFoundException.setErrorCode("InvalidCarrierGatewayID.NotFound");

        when(proxy.injectCredentialsAndInvoke(any(DeleteCarrierGatewayRequest.class), any()))
                .thenThrow(notFoundException);

        final DeleteHandler handler = new DeleteHandler();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InProgress() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvoke(any(DeleteCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStarted_Success() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvoke(any(DeleteCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStarted_Failed() {
        final AmazonEC2Exception unauthorizedException = new AmazonEC2Exception("");
        unauthorizedException.setErrorCode("UnauthorizedOperation");


        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenThrow(unauthorizedException);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
