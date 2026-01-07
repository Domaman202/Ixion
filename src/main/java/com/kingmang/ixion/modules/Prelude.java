package com.kingmang.ixion.modules;

import com.kingmang.ixion.exception.Panic;
import com.kingmang.ixion.runtime.CollectionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;


@SuppressWarnings("unused")
public class Prelude {

    public static void panic(Object msg) {
        new Panic((String) msg).send();
    }

	public static void println(Object arg) {
		System.out.println(arg.toString());
	}

	public static void print(Object arg) {
		System.out.print(arg.toString());
	}

    public static String readLine() {
        var scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            var s = scanner.nextLine();
            return s;
        }
        scanner.close();
        return null;
    }

    public static <T> void push(List<T> r, T a) {
        r.add(a);
    }

    public static <T> T pop(List<T> r) {
        return r.remove(r.size() - 1);
    }


    public static int len(Object r) {
		if(r instanceof CollectionUtil.IxListWrapper list)
            return list.list().size() + 1;
	    else if (r instanceof String s)
            return s.length();

        return -1;
    }
}
