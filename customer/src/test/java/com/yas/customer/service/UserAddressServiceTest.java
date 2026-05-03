package com.yas.customer.service;

import static com.yas.customer.util.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    private UserAddressService userAddressService;

    @BeforeEach
    void setUp() {
        userAddressService = new UserAddressService(userAddressRepository, locationService);
    }

    @Test
    void testGetUserAddressList_whenAuthenticated_returnsSortedActiveAddresses() {
        setUpSecurityContext("test-user");

        UserAddress inactiveAddress = UserAddress.builder()
            .id(1L)
            .userId("test-user")
            .addressId(100L)
            .isActive(false)
            .build();
        UserAddress activeAddress = UserAddress.builder()
            .id(2L)
            .userId("test-user")
            .addressId(200L)
            .isActive(true)
            .build();

        AddressDetailVm inactiveDetail = new AddressDetailVm(
            100L, "Inactive", "111", "Street 1", "City 1", "10000",
            1L, "District 1", 11L, "State 1", 21L, "Country 1"
        );
        AddressDetailVm activeDetail = new AddressDetailVm(
            200L, "Active", "222", "Street 2", "City 2", "20000",
            2L, "District 2", 12L, "State 2", 22L, "Country 2"
        );

        when(userAddressRepository.findAllByUserId("test-user")).thenReturn(List.of(inactiveAddress, activeAddress));
        when(locationService.getAddressesByIdList(List.of(100L, 200L))).thenReturn(List.of(inactiveDetail, activeDetail));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().isActive()).isTrue();
        assertThat(result.getLast().isActive()).isFalse();
        assertThat(result.getFirst().id()).isEqualTo(200L);
        assertThat(result.getLast().id()).isEqualTo(100L);
    }

    @Test
    void testGetUserAddressList_whenAnonymous_throwsAccessDeniedException() {
        setUpSecurityContext("anonymousUser");

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> userAddressService.getUserAddressList());

        assertThat(thrown.getMessage()).contains("ACTION FAILED, PLEASE LOGIN");
    }

    @Test
    void testGetAddressDefault_whenAnonymous_throwsAccessDeniedException() {
        setUpSecurityContext("anonymousUser");

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> userAddressService.getAddressDefault());

        assertThat(thrown.getMessage()).contains("ACTION FAILED, PLEASE LOGIN");
    }

    @Test
    void testGetAddressDefault_whenNoDefaultAddress_throwsNotFoundException() {
        setUpSecurityContext("test-user");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> userAddressService.getAddressDefault());

        assertThat(thrown.getMessage()).contains("User address not found");
    }

    @Test
    void testGetAddressDefault_whenDefaultAddressExists_returnsAddressDetail() {
        setUpSecurityContext("test-user");
        UserAddress userAddress = UserAddress.builder()
            .id(1L)
            .userId("test-user")
            .addressId(300L)
            .isActive(true)
            .build();
        AddressDetailVm addressDetailVm = new AddressDetailVm(
            300L, "Default", "333", "Street 3", "City 3", "30000",
            3L, "District 3", 13L, "State 3", 23L, "Country 3"
        );

        when(userAddressRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Optional.of(userAddress));
        when(locationService.getAddressById(300L)).thenReturn(addressDetailVm);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertThat(result).isEqualTo(addressDetailVm);
    }

    @Test
    void testCreateAddress_whenFirstAddress_marksItActive() {
        setUpSecurityContext("test-user");
        AddressPostVm addressPostVm = new AddressPostVm(
            "Contact", "123", "Street", "City", "10000", 1L, 2L, 3L
        );
        AddressVm addressVm = new AddressVm(10L, "Contact", "123", "Street", "City", "10000", 1L, 2L, 3L);

        when(userAddressRepository.findAllByUserId("test-user")).thenReturn(List.of());
        when(locationService.createAddress(addressPostVm)).thenReturn(addressVm);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAddressVm result = userAddressService.createAddress(addressPostVm);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(result.isActive()).isTrue();
        assertThat(result.userId()).isEqualTo("test-user");
        assertThat(result.addressGetVm()).isEqualTo(addressVm);
    }

    @Test
    void testCreateAddress_whenUserAlreadyHasAddress_marksNewAddressInactive() {
        setUpSecurityContext("test-user");
        AddressPostVm addressPostVm = new AddressPostVm(
            "Contact", "123", "Street", "City", "10000", 1L, 2L, 3L
        );
        AddressVm addressVm = new AddressVm(11L, "Contact", "123", "Street", "City", "10000", 1L, 2L, 3L);

        when(userAddressRepository.findAllByUserId("test-user")).thenReturn(List.of(UserAddress.builder().build()));
        when(locationService.createAddress(addressPostVm)).thenReturn(addressVm);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAddressVm result = userAddressService.createAddress(addressPostVm);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void testDeleteAddress_whenAddressNotFound_throwsNotFoundException() {
        setUpSecurityContext("test-user");
        when(userAddressRepository.findOneByUserIdAndAddressId("test-user", 99L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userAddressService.deleteAddress(99L));
    }

    @Test
    void testChooseDefaultAddress_marksMatchingAddressActive() {
        setUpSecurityContext("test-user");
        UserAddress address1 = UserAddress.builder().id(1L).userId("test-user").addressId(100L).isActive(false).build();
        UserAddress address2 = UserAddress.builder().id(2L).userId("test-user").addressId(200L).isActive(false).build();
        when(userAddressRepository.findAllByUserId("test-user")).thenReturn(List.of(address1, address2));

        userAddressService.chooseDefaultAddress(200L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserAddress>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(userAddressRepository).saveAll(captor.capture());
        List<UserAddress> savedAddresses = captor.getValue();
        assertThat(savedAddresses).extracting(UserAddress::getAddressId, UserAddress::getIsActive)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(100L, false),
                org.assertj.core.groups.Tuple.tuple(200L, true)
            );
    }
}