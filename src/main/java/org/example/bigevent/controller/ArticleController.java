package org.example.bigevent.controller;

import org.apache.ibatis.annotations.Delete;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    @PostMapping
    public Result addArticle(@RequestBody @Validated Article article){
        articleService.addArticle(article);
        return Result.success();
    }

    @GetMapping
    public Result<PageBean<Article>> List(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String state
    ){
        PageBean<Article> articlePageBean = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(articlePageBean);
    }
    //更新文章
    @PutMapping
    public Result update(@RequestBody @Validated Article article){
        articleService.update(article);
        return Result.success();
    }

    //获取文章详情
    @GetMapping("/detail")
    public Result<Article> detail(@RequestParam Integer id){
        Article article = articleService.detail(id);
        if (article!=null){
            return Result.success(article);
        }
        return Result.error("文章不存在");
    }

    //删除文章
    @DeleteMapping
    public Result delete(@RequestParam Integer id){
        Article article = articleService.detail(id);
        if (article!=null){
            articleService.delete(id);
            return Result.success();
        }
        return Result.error("文章不存在");
    }

    //获取轮播图文章列表
    @GetMapping("/carousel")
    public Result<List<Article>> getCarouselList(@RequestParam(required = false) Integer limit){
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        List<Article> carouselList = articleService.getCarouselList(limit);
        return Result.success(carouselList);
    }

    //获取文章统计信息
    @GetMapping("/stats")
    public Result<Map<String, Object>> getArticleStats(@RequestParam Integer articleId){
        Map<String, Object> stats = articleService.getArticleStats(articleId);
        if (stats != null) {
            return Result.success(stats);
        }
        return Result.error("文章不存在");
    }

    //计算文章热度
    @GetMapping("/heat")
    public Result<Integer> calculateHeat(@RequestParam Integer articleId){
        Integer heat = articleService.calculateHeat(articleId);
        return Result.success(heat);
    }
}

