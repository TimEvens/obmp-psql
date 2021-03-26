/*
 * Copyright (c) 2018 Tim Evens (tim@evensweb.com).  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.psqlquery;

import org.openbmp.api.helpers.IpAddr;
import org.openbmp.api.parsed.message.MsgBusFields;

import java.util.List;
import java.util.Map;


public class L3VpnPrefixQuery extends Query{

	public L3VpnPrefixQuery(List<Map<String, Object>> rowMap){
		
		this.rowMap = rowMap;
	}
	
	
    /**
     * Generate MySQL insert/update statement, sans the values
     *
     * @return Two strings are returned
     *      0 = Insert statement string up to VALUES keyword
     *      1 = ON DUPLICATE KEY UPDATE ...  or empty if not used.
     */
    public String[] genInsertStatement() {
        String [] stmt = { " INSERT IGNORE INTO l3vpn_rib (hash_id,peer_hash_id,path_attr_hash_id,isIPv4," +
                           "origin_as,prefix,prefix_len,prefix_bin,prefix_bcast_bin,prefix_bits,timestamp," +
                           "isWithdrawn,path_id,labels,isPrePolicy,isAdjRibIn,rd) VALUES ",

                           " ON DUPLICATE KEY UPDATE timestamp=values(timestamp)," +
                               "prefix_bits=values(prefix_bits)," +
                               "path_attr_hash_id=if(values(isWithdrawn) = 1, path_attr_hash_id, values(path_attr_hash_id))," +
                               "origin_as=if(values(isWithdrawn) = 1, origin_as, values(origin_as)),isWithdrawn=values(isWithdrawn)," +
                               "path_id=values(path_id), labels=values(labels)," +
                               "isPrePolicy=values(isPrePolicy), isAdjRibIn=values(isAdjRibIn),rd=values(rd) "
                        };
        return stmt;
    }

    /**
     * Generate bulk values statement for SQL bulk insert.
     *
     * @return String in the format of (col1, col2, ...)[,...]
     */
    public String genValuesStatement() {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i < rowMap.size(); i++) {
            if (i > 0)
                sb.append(',');

            sb.append('(');
            sb.append("'" + lookupValue(MsgBusFields.HASH, i) + "',");
            sb.append("'" + lookupValue(MsgBusFields.PEER_HASH, i) + "',");
            sb.append("'" + lookupValue(MsgBusFields.BASE_ATTR_HASH, i) + "',");
            sb.append(lookupValue(MsgBusFields.IS_IPV4, i) + ",");
            sb.append(lookupValue(MsgBusFields.ORIGIN_AS, i) + ",");
            sb.append("'" + lookupValue(MsgBusFields.PREFIX, i) + "',");
            sb.append(lookupValue(MsgBusFields.PREFIX_LEN, i) + ",");

            sb.append("X'" + IpAddr.getIpHex((String) lookupValue(MsgBusFields.PREFIX, i)) + "',");
            sb.append("X'" + IpAddr.getIpBroadcastHex((String) lookupValue(MsgBusFields.PREFIX, i), (Integer) lookupValue(MsgBusFields.PREFIX_LEN, i)) + "',");

            try {
                sb.append("'" + IpAddr.getIpBits((String) lookupValue(MsgBusFields.PREFIX, i)).substring(0,(Integer)lookupValue(MsgBusFields.PREFIX_LEN, i)) + "',");
            } catch (StringIndexOutOfBoundsException e) {

                //TODO: Fix getIpBits to support mapped IPv4 addresses in IPv6 (::ffff:ipv4)
                System.out.println("IP prefix failed to convert to bits: " +
                        (String) lookupValue(MsgBusFields.PREFIX, i) + " len: " + (Integer) lookupValue(MsgBusFields.PREFIX_LEN, i));
                sb.append("'',");
            }

            sb.append("'" + lookupValue(MsgBusFields.TIMESTAMP, i) + "',");
            sb.append((((String)lookupValue(MsgBusFields.ACTION, i)).equalsIgnoreCase("del") ? 1 : 0) + ",");
            sb.append(lookupValue(MsgBusFields.PATH_ID, i) + ",");
            sb.append("'" + lookupValue(MsgBusFields.LABELS, i) + "',");
            sb.append(lookupValue(MsgBusFields.ISPREPOLICY, i) + ",");
            sb.append(lookupValue(MsgBusFields.IS_ADJ_RIB_IN, i) + ",");
            sb.append("'" + lookupValue(MsgBusFields.VPN_RD, i) + "'");

            sb.append(')');
        }

        return sb.toString();
    }

}
