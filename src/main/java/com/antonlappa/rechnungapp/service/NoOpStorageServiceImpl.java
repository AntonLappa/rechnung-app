package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${app.storage.endpoint:}'.isEmpty()")
public class NoOpStorageServiceImpl implements StorageService {

    @Override
    public void upload(String key, byte[] data, String contentType) {
        throw new BusinessRuleException("File storage is not configured on this server.");
    }

    @Override
    public byte[] download(String key) {
        throw new BusinessRuleException("File storage is not configured on this server.");
    }

    @Override
    public void delete(String key) {
        // safe no-op — nothing to delete
    }
}
