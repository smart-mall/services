package product.web;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import product.entity.CategoryEntity;
import product.service.CategoryService;
import product.vo.Catalog2Vo;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class IndexController {

    @Resource
    private CategoryService categoryService;

    @GetMapping(value = {"/","/index.html"})
    private String indexPage(Model model) {
        //1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();
        model.addAttribute("categories",categoryEntities);

        return "index";
    }


    @GetMapping(value = "/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        return categoryService.getCatalogJson();

    }

}
