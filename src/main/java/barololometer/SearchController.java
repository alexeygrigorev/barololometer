package barololometer;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import barololometer.service.CombinedSearchResults;
import barololometer.service.SearchCombiner;
import barololometer.service.SessionExpiredException;

@Controller
@RequestMapping("/")
public class SearchController {

    private final SearchCombiner search;

    @Autowired
    public SearchController(SearchCombiner search) {
        this.search = search;
    }

    @RequestMapping("search")
    public String search(@RequestParam("q") String query, Map<String, Object> model) {
        CombinedSearchResults result = search.firstPageSearch(query);
        model.put("res", result);
        return "search";
    }

    @ResponseBody
    @RequestMapping("rest/search")
    public CombinedSearchResults restSearch(@RequestParam("q") String query, Map<String, ?> model) {
        return search.firstPageSearch(query);
    }

    @ResponseBody
    @RequestMapping("rest/next")
    public CombinedSearchResults next(@RequestParam("uuid") String uuid) throws SessionExpiredException {
        return search.moreResults(uuid);
    }

}
