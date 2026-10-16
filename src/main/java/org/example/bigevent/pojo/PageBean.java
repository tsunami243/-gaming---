package org.example.bigevent.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//分页返回结果对象
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 通用分页封装：总数 + 当前页数据。
 *
 * @param <T> 列表元素类型
 */
public class PageBean<T> {
    private Long total;//总条数
    private List<T> items;//当前页数据集合
}
