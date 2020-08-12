package com.amazonaws.ec2.carriergateway;

import java.util.Collections;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel model = ResourceModel.builder()
            .carrierGatewayId(CAGW_ID)
            .vpcId(VPC_ID)
            .build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();

        final DescribeCarrierGatewaysResult resultWithToken = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList())
                .withNextToken("token");

        final DescribeCarrierGatewaysResult resultWithCagw = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);

        when(proxy.injectCredentialsAndInvoke(any(), any()))
                .thenReturn(resultWithToken)
                .thenReturn(resultWithCagw);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_EmptyResults_Fails() {
        final ReadHandler handler = new ReadHandler();

        final DescribeCarrierGatewaysResult response = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        when(proxy.injectCredentialsAndInvoke(any(), any()))
                .thenReturn(response);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_NotFound_Fails() {
        final AmazonEC2Exception notFoundException = new AmazonEC2Exception("");
        notFoundException.setErrorCode("InvalidCarrierGatewayID.NotFound");

        final ReadHandler handler = new ReadHandler();

        final DescribeCarrierGatewaysResult response = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        when(proxy.injectCredentialsAndInvoke(any(), any()))
                .thenThrow(notFoundException);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_MultipleCagws_Fails() {
        final ReadHandler handler = new ReadHandler();

        final DescribeCarrierGatewaysResult response = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW, TEST_CAGW);

        when(proxy.injectCredentialsAndInvoke(any(), any()))
                .thenReturn(response);

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
