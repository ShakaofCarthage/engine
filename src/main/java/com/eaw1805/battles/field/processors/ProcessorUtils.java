package com.eaw1805.battles.field.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProcessorUtils {
	public static <T> T getRandom(Set<T> set) {
		List<T> list = new ArrayList<T>(set);
		Collections.shuffle(list);
		return list.get(0);
	}
}
