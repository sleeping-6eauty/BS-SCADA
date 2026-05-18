package com.example.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.domain.Equipment;

@Mapper
public interface UserEquipmentMapper {
    List<Equipment> findEquipmentsByUserId(@Param("userId") Long userId);
    int assignEquipmentToUser(@Param("userId") Long userId, @Param("equipmentId") String equipmentId);
    int assignEquipmentsToUser(@Param("userId") Long userId, @Param("equipmentIds") List<String> equipmentIds);
}
