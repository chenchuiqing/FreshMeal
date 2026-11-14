package com.freshmeal.service;

import com.freshmeal.dto.user.AddressSaveRequest;
import com.freshmeal.vo.user.AddressVO;
import java.util.List;

public interface AddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressSaveRequest request);

    AddressVO update(Long userId, Long addressId, AddressSaveRequest request);

    void delete(Long userId, Long addressId);

    AddressVO setDefault(Long userId, Long addressId);
}
