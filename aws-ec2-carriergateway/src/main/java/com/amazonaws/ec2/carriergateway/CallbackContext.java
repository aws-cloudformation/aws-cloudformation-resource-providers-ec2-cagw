package com.amazonaws.ec2.carriergateway;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CallbackContext {
    private boolean createStarted;
    private boolean deleteStarted;
    private boolean updateStarted;

    private Set<Tag> tagsToCreate;
    private Set<Tag> tagsToDelete;
}