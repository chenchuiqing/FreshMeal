package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.user.AddressSaveRequest;
import com.freshmeal.entity.Address;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.AddressMapper;
import com.freshmeal.service.AddressService;
import com.freshmeal.vo.user.AddressVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdateTime))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressSaveRequest request) {
        boolean hasDefault = addressMapper.exists(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
        boolean shouldBeDefault = request.isDefault() || !hasDefault;
        if (shouldBeDefault) {
            clearDefault(userId, null);
        }

        Address address = fromRequest(userId, request);
        address.setIsDefault(shouldBeDefault ? 1 : 0);
        addressMapper.insert(address);
        return toVo(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long addressId, AddressSaveRequest request) {
        Address current = requireOwned(userId, addressId);
        boolean demoteCurrentDefault = current.getIsDefault() == 1
                && !request.isDefault();
        if (demoteCurrentDefault
                && addressMapper.selectCount(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .ne(Address::getId, addressId)) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "至少保留一个默认地址");
        }
        if (request.isDefault()) {
            clearDefault(userId, addressId);
        }

        Address updated = fromRequest(userId, request);
        updated.setId(addressId);
        updated.setIsDefault(request.isDefault() ? 1 : 0);
        addressMapper.updateById(updated);
        if (demoteCurrentDefault) {
            Address nextDefault = addressMapper.selectOne(
                    Wrappers.<Address>lambdaQuery()
                            .eq(Address::getUserId, userId)
                            .eq(Address::getIsDefault, 1)
                            .orderByDesc(Address::getUpdateTime)
                            .last("LIMIT 1"));
            if (nextDefault != null) {
                nextDefault.setIsDefault(1);
                addressMapper.updateById(nextDefault);
            }
        }
        return toVo(requireOwned(userId, addressId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long addressId) {
        Address current = requireOwned(userId, addressId);
        addressMapper.deleteById(addressId);
        if (current.getIsDefault() == 1) {
            Address next = addressMapper.selectOne(Wrappers.<Address>lambdaQuery()
                            .eq(Address::getUserId, userId)
                            .orderByDesc(Address::getUpdateTime)
                            .last("LIMIT 1"));
            if (next != null) {
                next.setIsDefault(1);
                addressMapper.updateById(next);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO setDefault(Long userId, Long addressId) {
        requireOwned(userId, addressId);
        clearDefault(userId, addressId);
        Address address = new Address();
        address.setId(addressId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return toVo(requireOwned(userId, addressId));
    }

    private Address requireOwned(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND.getCode(),
                    ErrorCode.ADDRESS_NOT_FOUND.getMessage());
        }
        return address;
    }

    private void clearDefault(Long userId, Long exceptId) {
        Address reset = new Address();
        reset.setIsDefault(0);
        addressMapper.update(reset, Wrappers.<Address>lambdaUpdate()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .ne(exceptId != null, Address::getId, exceptId));
    }

    private Address fromRequest(Long userId, AddressSaveRequest request) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiver(request.receiver().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district().trim());
        address.setDetail(request.detail().trim());
        address.setLongitude(request.longitude());
        address.setLatitude(request.latitude());
        address.setTag(StringUtils.hasText(request.tag()) ? request.tag().trim() : null);
        address.setDeleted(0);
        return address;
    }

    private AddressVO toVo(Address address) {
        String fullAddress = address.getProvince() + address.getCity()
                + address.getDistrict() + address.getDetail();
        return new AddressVO(
                address.getId(),
                address.getReceiver(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                fullAddress,
                address.getLongitude(),
                address.getLatitude(),
                address.getTag(),
                address.getIsDefault() == 1
        );
    }
}
