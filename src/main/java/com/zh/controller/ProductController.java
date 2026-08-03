package com.zh.controller;


import com.zh.domain.dto.ProductDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Product;
import com.zh.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private IProductService productService;

    @GetMapping
    public Result search(@RequestParam(value = "name", required = false) String name,
                         @RequestParam(value = "page", defaultValue = "0") Integer page,
                         @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return productService.searchProducts(name, page, size);
    }

    @PostMapping
    public Result add(@RequestBody ProductDTO dto) {
        return productService.addProduct(dto);
    }


    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        return productService.updateProduct(id, dto);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }
}
