package com.taro.sampleservice.controllers;


import com.taro.sampleservice.models.Product;
import com.taro.sampleservice.services.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.print.URIException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@RestController
public class ProductController {
    private static final Logger logger = LogManager.getLogger();
    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Integer id) {

        return productService.findById(id).map(
                product -> {
                    try{
                        return ResponseEntity
                                .ok()
                                .eTag(Integer.toString(product.getVersion()))
                                .location(new URI("/product/"+product.getId()))
                                .body(product);
                    }
                    catch(URISyntaxException e){
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                }
        ).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    public Iterable<Product> getProducts(){
        return productService.findAll();
    }

    @PostMapping("/product")
    public ResponseEntity<Product> createProduct(@RequestBody Product product){
        logger.info("Creating a new product with name: {}, quantity: {} ",product.getName(),product.getQuantity());

        // Create the new product
        Product newProduct = productService.save(product);

        try{
            return ResponseEntity
                    .created(new URI("/product/" + newProduct.getId()))
                    .eTag(Integer.toString(newProduct.getVersion()))
                    .body(newProduct);
        } catch(URISyntaxException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/product/{id}")
    public ResponseEntity<?> updateProduct(@RequestBody Product product,
                                           @PathVariable Integer id,
                                           @RequestHeader("If-Match") Integer ifMatch) {
        logger.info("Updating product with id: {}, name: {}, quantity: {}",
                id,product.getName(),product.getQuantity());

        Optional<Product> existingProduct = productService.findById(id);

        return existingProduct.map(p ->{
            logger.info("Product with id: {} has a version: {}. Update is for If-Match: {}",
                    id,p.getVersion(),ifMatch);

            if(!p.getVersion().equals(ifMatch)){
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            p.setName(p.getName());
            p.setQuantity(p.getQuantity());
            p.setVersion(p.getVersion() + 1);

            logger.info("Updating product with ID: {} -> name={}, quantity={}, version={}",
                    p.getId(),p.getName(),p.getQuantity(),p.getVersion());
            try{
                if(productService.update(p)){
                    return ResponseEntity
                            .ok()
                            .location(new URI("/product/" + p.getId()))
                            .eTag(Integer.toString(p.getVersion()))
                            .body(p);
                }
                else{
                    return ResponseEntity.notFound().build();
                }
            }
            catch(URISyntaxException e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }).orElse(ResponseEntity.notFound().build());

    }


}
