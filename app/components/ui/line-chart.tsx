import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, Dimensions, TouchableOpacity } from 'react-native';
import Svg, { Path, Defs, LinearGradient, Stop, Circle, Line, Text as SvgText, G, Rect } from 'react-native-svg';

interface DualLineChartProps {
    data1: number[];
    data2: number[];
    labels: string[];
    color1?: string;
    color2?: string;
    height?: number;
}

export function DualLineChart({
    data1,
    data2,
    labels,
    color1 = '#2E7D32',
    color2 = '#F57C00',
    height = 120,
}: DualLineChartProps) {
    const [selectedIndex, setSelectedIndex] = useState<number | null>(labels.length > 0 ? labels.length - 1 : null);

    useEffect(() => {
        if (labels.length > 0) {
            setSelectedIndex(labels.length - 1);
        }
    }, [labels]);

    const width = Dimensions.get('window').width - 64;
    // Removed extraBottomSpace logic to let container grow naturally
    const padding = { top: 30, bottom: 30, left: 20, right: 20 };
    const chartHeight = height - padding.top - padding.bottom;
    const chartWidth = width - padding.left - padding.right;

    // Unique IDs for gradients
    const gradientId1 = `gradient1-${Math.random().toString(36).substr(2, 9)}`;
    const gradientId2 = `gradient2-${Math.random().toString(36).substr(2, 9)}`;

    const allData = [...data1, ...data2];
    const maxValue = Math.max(...allData);
    const minValue = Math.min(...allData);
    const range = maxValue - minValue || 1;

    const getPoints = (data: number[]) => data.map((value, index) => ({
        x: padding.left + (index / (data.length - 1)) * chartWidth,
        y: padding.top + chartHeight - ((value - minValue) / range) * chartHeight,
        value,
    }));

    const points1 = getPoints(data1);
    const points2 = getPoints(data2);

    const createLinePath = (points: typeof points1) =>
        points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');

    const createAreaPath = (points: typeof points1) =>
        `${createLinePath(points)} L ${points[points.length - 1].x} ${padding.top + chartHeight} L ${points[0].x} ${padding.top + chartHeight} Z`;

    const handlePress = (index: number) => {
        setSelectedIndex(index === selectedIndex ? null : index);
    };

    return (
        <View style={styles.container}>
            <Svg width={width} height={height}>
                <Defs>
                    <LinearGradient id={gradientId1} x1="0%" y1="0%" x2="0%" y2="100%">
                        <Stop offset="0%" stopColor={color1} stopOpacity="0.2" />
                        <Stop offset="100%" stopColor={color1} stopOpacity="0" />
                    </LinearGradient>
                    <LinearGradient id={gradientId2} x1="0%" y1="0%" x2="0%" y2="100%">
                        <Stop offset="0%" stopColor={color2} stopOpacity="0.2" />
                        <Stop offset="100%" stopColor={color2} stopOpacity="0" />
                    </LinearGradient>
                </Defs>

                {/* Grid lines */}
                {[0, 0.5, 1].map((ratio, index) => (
                    <Line
                        key={index}
                        x1={padding.left}
                        y1={padding.top + chartHeight * ratio}
                        x2={width - padding.right}
                        y2={padding.top + chartHeight * ratio}
                        stroke="#E0E0E0"
                        strokeWidth={1}
                        strokeDasharray="3,3"
                    />
                ))}

                {/* Area fills */}
                <Path d={createAreaPath(points1)} fill={`url(#${gradientId1})`} />
                <Path d={createAreaPath(points2)} fill={`url(#${gradientId2})`} />

                {/* Lines */}
                <Path d={createLinePath(points1)} stroke={color1} strokeWidth={2.5} fill="none" strokeLinecap="round" />
                <Path d={createLinePath(points2)} stroke={color2} strokeWidth={2.5} fill="none" strokeLinecap="round" />

                {/* Interactive Dots and values */}
                {(points1.length > 0 ? points1 : points2).map((_, index) => {
                    const isSelected = selectedIndex === index;
                    const isLast = index === points1.length - 1;
                    const showValueForPoint = isSelected || (selectedIndex === null && isLast);

                    // Collision detection for labels
                    const p1 = points1[index];
                    const p2 = points2[index];
                    const distance = Math.abs(p1.y - p2.y);
                    const offset = 18;
                    // If too close, push labels away from each other
                    const isTooClose = distance < 35;
                    const y1Label = isTooClose && p1.y < p2.y ? p1.y - offset - 12 : p1.y - offset;
                    const y2Label = isTooClose && p2.y < p1.y ? p2.y - offset - 12 : (isTooClose ? p2.y + offset + 12 : p2.y - offset);

                    // Ensure Y2 label doesn't go below chart
                    const safeY2Label = y2Label > height - 15 ? p2.y - offset : y2Label;

                    // Smart value formatting function
                    const formatValue = (value: number): string => {
                        if (value >= 100000) return `${(value / 100000).toFixed(1)}L`;
                        if (value >= 1000) return `${Math.round(value / 1000)}K`;
                        return `${Math.round(value)}`;
                    };

                    // Calculate dynamic width based on value length
                    const getLabel1Width = () => {
                        const formatted = formatValue(p1.value);
                        return Math.max(24, formatted.length * 7 + 8);
                    };
                    const getLabel2Width = () => {
                        const formatted = formatValue(p2.value);
                        return Math.max(24, formatted.length * 7 + 8);
                    };

                    return (
                        <G key={index} onPress={() => handlePress(index)}>
                            {/* Larger transparent touch target covering both lines vertical space potentially */}
                            <Rect
                                x={points1[index].x - 15}
                                y={0}
                                width={30}
                                height={height}
                                fill="transparent"
                            />

                            {/* Visible dots for both lines */}
                            <Circle
                                cx={points1[index].x}
                                cy={points1[index].y}
                                r={isSelected ? 6 : 4}
                                fill="#FFFFFF"
                                stroke={color1}
                                strokeWidth={2}
                            />
                            <Circle
                                cx={points2[index].x}
                                cy={points2[index].y}
                                r={isSelected ? 6 : 4}
                                fill="#FFFFFF"
                                stroke={color2}
                                strokeWidth={2}
                            />

                            {/* Value Labels */}
                            {showValueForPoint && (
                                <G>
                                    {/* Label for Line 1 - Pill shape */}
                                    <Rect
                                        x={points1[index].x - getLabel1Width() / 2}
                                        y={y1Label - 14}
                                        width={getLabel1Width()}
                                        height={20}
                                        rx={10}
                                        fill={color1}
                                    />
                                    <SvgText x={points1[index].x} y={y1Label} fill="#FFFFFF" fontSize={10} fontWeight="600" textAnchor="middle">
                                        {formatValue(p1.value)}
                                    </SvgText>

                                    {/* Label for Line 2 - Pill shape */}
                                    <Rect
                                        x={points2[index].x - getLabel2Width() / 2}
                                        y={safeY2Label - 14}
                                        width={getLabel2Width()}
                                        height={20}
                                        rx={10}
                                        fill={color2}
                                    />
                                    <SvgText x={points2[index].x} y={safeY2Label} fill="#FFFFFF" fontSize={10} fontWeight="600" textAnchor="middle">
                                        {formatValue(p2.value)}
                                    </SvgText>
                                </G>
                            )}
                        </G>
                    );
                })}

                {/* X-axis labels */}
                {labels.map((label, index) => (
                    <SvgText
                        key={index}
                        x={padding.left + (index / (labels.length - 1)) * chartWidth}
                        y={height - 8}
                        fill={selectedIndex === index ? '#212121' : '#757575'}
                        fontSize={selectedIndex === index ? 11 : 10}
                        fontWeight={selectedIndex === index ? '600' : '400'}
                        textAnchor="middle"
                    >
                        {label}
                    </SvgText>
                ))}
            </Svg>

            {/* Selected Point Info Display - Native View */}
            {selectedIndex !== null && (
                <View style={styles.selectedInfoContainer}>
                    {/* <Text style={styles.selectedLabel}>{labels[selectedIndex]}</Text> */}
                    <View style={styles.summaryRow}>
                        <View style={styles.summaryItem}>
                            <View style={[styles.summaryDot, { backgroundColor: color1 }]} />
                            <Text style={styles.summaryLabel}>बिक्री:</Text>
                            <Text style={[styles.summaryValue, { color: color1 }]}>
                                ₹{data1[selectedIndex]?.toLocaleString('en-IN') || 0}
                            </Text>
                        </View>
                        <View style={styles.summaryItem}>
                            <View style={[styles.summaryDot, { backgroundColor: color2 }]} />
                            <Text style={styles.summaryLabel}>लाभ:</Text>
                            <Text style={[styles.summaryValue, { color: data2[selectedIndex] >= 0 ? color2 : '#D32F2F' }]}>
                                ₹{data2[selectedIndex]?.toLocaleString('en-IN') || 0}
                            </Text>
                        </View>
                    </View>
                </View>
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        justifyContent: 'center',
    },
    selectedInfoContainer: {
        width: '100%',
        paddingTop: 12,
        borderTopWidth: 1,
        borderTopColor: '#EEEEEE',
        marginTop: 4,
        alignItems: 'center',
    },
    selectedLabel: {
        fontSize: 14,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 8,
    },
    summaryRow: {
        flexDirection: 'row',
        width: '100%',
        justifyContent: 'center',
        gap: 12,
    },
    summaryItem: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    summaryDot: {
        width: 10,
        height: 10,
        borderRadius: 5,
        marginRight: 6,
    },
    summaryLabel: {
        fontSize: 12,
        color: '#616161',
        marginRight: 4,
    },
    summaryValue: {
        fontSize: 13,
        fontWeight: '700',
    },
});
