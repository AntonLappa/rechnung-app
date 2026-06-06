package com.antonlappa.rechnungapp.service;

public interface StorageService {
    void upload(String key, byte[] data, String contentType);
    byte[] download(String key);
    void delete(String key);
}
