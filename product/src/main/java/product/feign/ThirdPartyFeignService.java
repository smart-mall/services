package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient("third-party")
public interface ThirdPartyFeignService {
    @DeleteMapping("/thirdParty/minio/deleteFile")
    R deleteFile(@RequestBody List<String> https);
}
