package com.ruoyi.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MutableHeaderRequestWrapper extends HttpServletRequestWrapper
{
    private final Map<String, String> headers = new LinkedHashMap<>();

    private final Set<String> removedHeaders = new LinkedHashSet<>();

    public MutableHeaderRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    public void putHeader(String name, String value)
    {
        headers.put(normalize(name), value);
        removedHeaders.remove(normalize(name));
    }

    public void removeHeader(String name)
    {
        headers.remove(normalize(name));
        removedHeaders.add(normalize(name));
    }

    @Override
    public String getHeader(String name)
    {
        String normalizedName = normalize(name);
        if (removedHeaders.contains(normalizedName))
        {
            return null;
        }
        if (headers.containsKey(normalizedName))
        {
            return headers.get(normalizedName);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        String header = getHeader(name);
        if (header == null)
        {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Collections.singletonList(header));
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        List<String> names = Collections.list(super.getHeaderNames());
        names.removeIf(name -> removedHeaders.contains(normalize(name)));
        for (String name : headers.keySet())
        {
            if (names.stream().noneMatch(existing -> normalize(existing).equals(name)))
            {
                names.add(name);
            }
        }
        return Collections.enumeration(new ArrayList<>(names));
    }

    private String normalize(String name)
    {
        return name.toLowerCase(Locale.ROOT);
    }
}
