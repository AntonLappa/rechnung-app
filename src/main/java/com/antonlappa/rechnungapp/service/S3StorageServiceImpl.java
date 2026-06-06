package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public void upload(String key, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) data.length)
                        .build(),
                RequestBody.fromBytes(data));
    }

    @Override
    public byte[] download(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(key)
                        .build(),
                ResponseTransformer.toBytes()).asByteArray();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(key)
                        .build());
    }
}
