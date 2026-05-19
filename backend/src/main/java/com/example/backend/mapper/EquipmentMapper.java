package com.example.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.domain.Equipment;

@Mapper
public interface EquipmentMapper {
    List<Equipment> findAll();
    Equipment findByEquipmentCode(@Param("equipmentId") String equipmentId);
    int insert(Equipment equipment);
    int update(Equipment equipment);
    int deleteById(@Param("equipmentId") String equipmentId);
}
