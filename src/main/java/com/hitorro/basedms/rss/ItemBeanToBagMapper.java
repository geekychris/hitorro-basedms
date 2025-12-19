/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.rss;

import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.typesystem.Bag;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeManager;

/**
 *
 */
public class ItemBeanToBagMapper extends BaseMapper<ItemBean, Bag> {
    public static final ItemBeanToBagMapper map = new ItemBeanToBagMapper();
    private String typeString = "webdoc";
    private Type type;

    public ItemBeanToBagMapper() {
        TypeManager tm = TypeManager.getTypeManager();
        type = (Type) tm.getTypeByShortName(typeString);
    }

    @Override
    public Bag apply(final ItemBean e) {
        Bag bag = new Bag();

        bag.setType(type);
        bag.setValue("body", e.getDescription());
        bag.setValue("content", e.getContent());
        bag.setValue("url", e.getLink());
        bag.setValue("srcurl", e.getSrcUrl());
        bag.setValue("pubdate", e.getPubDate());
        bag.setValue("title", e.getTitle());
        bag.setValue("contenttype", e.getContentType());
        return bag;
    }
}
