/**
* Copyright 2013 Arcadia Consulting C.B.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/
package es.arcadiaconsulting.appstoresstats.android.model;

import es.arcadiaconsulting.appstoresstats.common.CommonStatsData;

public class StatsDataAndroid extends CommonStatsData {
	/**
	 * Indicates the number of errors reported by users
	 */
	private int errorNumber;
	/**
	 * Indicates the total number of comments
	 */
	private int ratingNumber;
	public StatsDataAndroid() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getErrorNumber() {
		return errorNumber;
	}
	public void setErrorNumber(int errorNumber) {
		this.errorNumber = errorNumber;
	}
	public int getRatingNumber() {
		return ratingNumber;
	}
	public void setRatingNumber(int ratingNumber) {
		this.ratingNumber = ratingNumber;
	}

}