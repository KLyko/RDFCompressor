package de.uni_leipzig.simba.util;

import java.util.LinkedList;
import java.util.List;

import orestes.bloomfilter.BloomFilter;

public class BloomTest {
	BloomFilter<Integer> bloom1 = new BloomFilter<Integer>(10, 0.1);
	BloomFilter<Integer> bloom2 = new BloomFilter<Integer>(10, 0.1);
	
	public void test1() {
		for(int i = 0; i<5; i++)
			bloom1.add(i);
		
		for(int i = 2; i<8; i++) {
			List<Integer> list2 = new LinkedList<Integer>();
			System.out.println("is "+i+" in bloom1="+bloom1.contains(i));
			bloom2 = new BloomFilter<Integer>(10, 0.1);
			for(int j=i; j<(i+3);j++) {
				list2.add(j);
				bloom2.add(j);
				
			}
			System.out.println("List "+list2+" is subset? "+bloom1.intersect(bloom2));
			System.out.println("List 1 contains all of "+list2+"? "+bloom1.containsAll(list2));
		}			
	}
	
	public static void main(String args[]) {
		BloomTest test = new BloomTest();
		test.test1();
	}
}
