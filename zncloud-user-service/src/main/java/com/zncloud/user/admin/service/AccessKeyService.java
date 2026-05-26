package com.zncloud.user.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.common.util.crypto.HmacSha256Util;
import com.zncloud.user.admin.dto.AccessKeyResponse;
import com.zncloud.user.admin.dto.CreateAccessKeyRequest;
import com.zncloud.user.admin.dto.CreateAccessKeyResponse;
import com.zncloud.user.admin.entity.AccessKeyEntity;
import com.zncloud.user.admin.enums.AccessKeyStatus;
import com.zncloud.user.admin.repository.AccessKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccessKeyService {

    @Value("${access-key.encrypt-secret:DefaultEncryptKeyForAKSK2024}")
    private String encryptSecret;

    @Value("${access-key.encrypt-salt:deadbeef}")
    private String encryptSalt;

    @Autowired
    private AccessKeyRepository accessKeyRepository;

    private TextEncryptor encryptor() {
        return Encryptors.text(encryptSecret, encryptSalt);
    }

    /**
     * 创建密钥对
     */
    @Transactional
    public CreateAccessKeyResponse createKey(CreateAccessKeyRequest request, Long createdBy) {
        AccessKeyEntity entity = new AccessKeyEntity();
        entity.setKeyId(HmacSha256Util.generateKeyId());
        String plainSecret = HmacSha256Util.generateKeySecret();
        entity.setKeySecret(encryptor().encrypt(plainSecret));
        entity.setName(request.getName());
        entity.setStatus(AccessKeyStatus.ACTIVE);
        entity.setPermissions(request.getPermissions());
        entity.setCreatedBy(createdBy);
        entity.setExpiredAt(request.getExpiredAt());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        accessKeyRepository.insert(entity);
        return CreateAccessKeyResponse.fromEntity(entity, plainSecret);
    }

    /**
     * 查询密钥列表
     */
    public List<AccessKeyResponse> listKeys(Long createdBy) {
        LambdaQueryWrapper<AccessKeyEntity> wrapper = new LambdaQueryWrapper<AccessKeyEntity>()
                .eq(createdBy != null, AccessKeyEntity::getCreatedBy, createdBy)
                .orderByDesc(AccessKeyEntity::getCreatedAt);
        return accessKeyRepository.selectList(wrapper)
                .stream()
                .map(e -> AccessKeyResponse.fromEntity(e, false))
                .collect(Collectors.toList());
    }

    /**
     * 吊销（删除）密钥
     */
    @Transactional
    public void revokeKey(Long id) {
        AccessKeyEntity entity = accessKeyRepository.selectById(id);
        if (entity == null) {
            throw new RuntimeException("密钥不存在");
        }
        accessKeyRepository.deleteById(id);
    }

    /**
     * 按 keyId 查找密钥（用于网关认证）
     */
    public AccessKeyEntity findByKeyId(String keyId) {
        return accessKeyRepository.findByKeyId(keyId).orElse(null);
    }

    /**
     * 验证 AK/SK 签名
     */
    public boolean verifySignature(String keyId, String signature, String stringToSign) {
        AccessKeyEntity entity = findByKeyId(keyId);
        if (entity == null || entity.getStatus() != AccessKeyStatus.ACTIVE) {
            return false;
        }
        // 检查是否过期
        if (entity.getExpiredAt() != null && entity.getExpiredAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        String plainSecret = encryptor().decrypt(entity.getKeySecret());
        return HmacSha256Util.verify(stringToSign, plainSecret, signature);
    }
}
