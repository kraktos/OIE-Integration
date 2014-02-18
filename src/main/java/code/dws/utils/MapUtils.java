package code.dws.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapUtils
{

    public static Map<String, Integer> sortByValue(Map<String, Integer> map)
    {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            result.put(entry.getKey(), (Integer) entry.getValue());
        }
        return result;
    }

    /**
     * overloaded function which sorts on the values, ascending or descending
     * 
     * @param map
     * @param ascending
     * @return
     */
    public static Map<String, Integer> sortByValue(Map<String, Integer> map, final boolean ascending)
    {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                if (ascending)
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
                else
                    return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());

            }
        });

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            result.put(entry.getKey(), (Integer) entry.getValue());
        }
        return result;
    }
}
